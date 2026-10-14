# Référence métier et API

## Conventions

Tous les chemins ci-dessous sont relatifs à `/stationSki` (port `8087`). Les identifiants sont strictement positifs et générés à la création. Les dates sont ISO (`2027-01-15`), les heures `09:00:00` et les instants locaux `2027-01-15T09:00:00`. Les `LocalDateTime` représentent l'heure locale de la station, sans décalage ; configurer les serveurs avec le même fuseau. La connexion MySQL utilise UTC pour les conversions JDBC.

Les périodes de planning, location et rapports sont semi-ouvertes `[from,to[` ; deux périodes qui se touchent ne se chevauchent pas. Les dates de validité des disponibilités sont inclusives. La recherche historique des abonnements conserve son `BETWEEN` inclusif sur la date de début.

- `400` : champs, dates, montants ou identifiants invalides ; JSON non lisible.
- `404` : ressource inexistante.
- `409` : doublon, créneau occupé, capacité, transition ou association incompatible ; conflit d'intégrité/concurrence.
- Les créations et modifications retournent la ressource avec `200`, conformément aux contrôleurs historiques ; les suppressions/annulations sans corps retournent `200`.

Exemple d'erreur :

```json
{"timestamp":"2027-01-15T09:00:00","status":409,"message":"Capacité du créneau atteinte","path":"/stationSki/registration/skier/1/slot/2"}
```

Les noms/prénoms/villes/codes/marques/tailles/motifs non vides sont limités à 255 caractères. Les tailles sont des libellés libres (ex. `42`, `170 cm`, `M`), car leur unité dépend du matériel. Les nouveaux montants sont des `BigDecimal` non négatifs à deux décimales maximum (`DECIMAL(12,2)`), dans une monnaie unique configurée par convention de la station. Paiements et remboursements exigent un montant strictement positif.

## Skieurs et pistes

Les routes historiques `/skier/add`, `/all`, `/get/{id}`, `/delete/{id}`, `/assignToSub/{skierId}/{subId}`, `/assignToPiste/{skierId}/{pisteId}`, `/getSkiersBySubscription?typeSubscription=MONTHLY` et `/addAndAssign/{courseId}` restent présentes, ainsi que `/piste/add`, `/all`, `/get/{id}`, `/delete/{id}`.

| Route ajoutée | Fonction |
| --- | --- |
| `PUT /skier/update` | Modifier `numSkier`, `firstName`, `lastName`, `dateOfBirth`, `city` |
| `GET /skier/search?name=&city=&page=0&size=20` | Recherche partielle insensible à la casse, taille 1–100 |
| `PUT /piste/update` | Modifier `numPiste`, `namePiste`, `color`, `length`, `slope` |
| `GET /piste/search?color=BLUE` | Filtrer par couleur/difficulté |
| `GET /piste/{id}/skiers` | Skieurs affectés |

La naissance doit être passée ; une piste a une longueur positive et une pente de 0 à 90 degrés. Les couleurs sont `GREEN`, `BLUE`, `RED`, `BLACK`. L'affectation répétée à une piste ou à un abonnement déjà attribué est un conflit. L'abonnement peut être omis à la création d'un skieur, fourni comme nouvel abonnement ou référencé par `numSub` existant ; les pistes sont affectées par leur route dédiée. Les modifications personnelles ne remplacent pas les associations.

La suppression d'un skieur est refusée dès qu'il possède un historique d'inscription (même annulée), de location ou de paiement. L'abonnement n'est plus supprimé en cascade. La suppression d'une piste affectée est refusée.

`POST /skier/addAndAssign/{courseId}` choisit le prochain créneau futur ouvert du cours et appelle le service d'inscriptions. L'opération complète est transactionnelle et échoue si aucun créneau n'est disponible ou si le skieur ne respecte pas les règles ; aucune collection d'inscriptions entrante n'est nécessaire.

## Cours et moniteurs

Les routes historiques de création, liste, lecture et modification restent présentes, dont `PUT /instructor/addAndAssignToCourse/{courseId}`.

| Route ajoutée | Fonction |
| --- | --- |
| `DELETE /course/delete/{id}` | Suppression sans associations ni historique |
| `GET /course/search?type=&support=&level=` | Filtres combinables |
| `GET /course/{id}/registrations` | Historique, annulations incluses |
| `GET /course/{id}/participants/count` | Skieurs distincts avec inscriptions non annulées |
| `GET /course/{id}/instructors` | Moniteur associé, sous forme de liste |
| `DELETE /instructor/delete/{id}` | Suppression sans cours, créneaux ni disponibilités |
| `PUT /instructor/{id}/courses/{courseId}` | Affecter un cours existant |
| `DELETE /instructor/{id}/courses/{courseId}` | Retirer l'association sans créneaux futurs non annulés |
| `GET /instructor/{id}/courses` | Cours associés |
| `GET /instructor/search?name=` | Recherche partielle par nom/prénom |

Le niveau est un entier positif ; le prix un flottant fini positif ou nul conservé pour compatibilité. Types : `INDIVIDUAL`, `COLLECTIVE_CHILDREN`, `COLLECTIVE_ADULT` ; supports : `SKI`, `SNOWBOARD`. La date d'embauche est présente ou passée. Un cours ayant des inscriptions ou créneaux ne peut plus changer de type, support ou niveau. La suppression d'un cours avec inscription, créneau (même passé/annulé) ou moniteur associé est refusée.

**Mapping conservé :** `Instructor.courses` reste le `@OneToMany` unidirectionnel existant, avec sa table de jointure et son unicité de cours. Un moniteur enseigne plusieurs cours ; un cours est affecté à un seul moniteur. Les services verrouillent le cours avant affectation et vérifient l'exclusivité. Le planning exige cette association. Une relation plusieurs-à-plusieurs n'était pas nécessaire et aurait changé les contraintes de la table existante.

`Course.timeSlot` reste stocké et accepté, mais est déprécié : il ne pilote plus les horaires. Aucun champ ni aucune colonne historique n'est supprimé.

## Planning et inscriptions

| Méthode et chemin | Paramètres / corps |
| --- | --- |
| `POST /planning/availability/{instructorId}` | `dayOfWeek`, `startTime`, `endTime`, `validFrom`, `validUntil` |
| `GET /planning/instructors/{id}/availability` | Disponibilités récurrentes |
| `POST /planning/slots` | `courseId`, `instructorId`, `startTime`, `endTime`, `maxCapacity` |
| `GET /planning/slots/{id}` | Détail |
| `GET /planning/slots?from=&to=&courseId=&instructorId=` | Période obligatoire, autres filtres optionnels |
| `GET /planning/slots/available?from=&to=&courseId=` | Créneaux futurs `OPEN` |
| `PUT /planning/slots/{id}` | Même corps qu'à la création |
| `DELETE /planning/slots/{id}` | Annuler et marquer les inscriptions annulées |
| `POST /registration/skier/{skierId}/slot/{slotId}` | Inscrire sans corps |
| `GET /registration/skier/{skierId}` | Historique |
| `GET /registration?from=&to=&courseId=` | Inscriptions datées, annulations incluses |
| `DELETE /registration/{id}` | Annulation logique |

Les créneaux sont futurs, de durée positive, sur une seule journée et contenus dans une disponibilité du moniteur (pas de fusion implicite de disponibilités adjacentes). Le verrou moniteur sérialise la vérification des chevauchements. Capacité individuelle : 1 ; capacité collective : 1 à 6. Statuts : `OPEN`, `FULL`, `CANCELLED`. Un créneau commencé ne peut plus être modifié/annulé. Un créneau avec historique d'inscriptions ne peut pas être déplacé : annuler et recréer explicitement.

L'âge est calculé au début du cours : moins de 16 ans en collectif enfants, 16 ans ou plus en collectif adultes. La règle historique d'une inscription non annulée par skieur/cours/semaine est conservée ; les créneaux permettent désormais de distinguer les années ISO. La capacité s'applique à chaque séance précise, plutôt qu'à toutes les séances d'un cours sur une semaine. Les inscriptions verrouillent d'abord le skieur, puis le créneau, avant de vérifier les chevauchements et la capacité. Les services critiques utilisent `READ_COMMITTED` pour éviter des lectures de capacité obsolètes après attente d'un verrou. L'annulation libère une place ; l'annulation du créneau conserve son historique.

**Compatibilité des inscriptions :** `numWeek` reste présent et est calculé depuis la date du créneau (semaine ISO). `planningSlot` est nullable uniquement pour les lignes historiques. Les anciennes routes d'inscription restent exposées, mais exigent désormais `{"planningSlot":{"id":2}}` pour une création ; la route incluant le cours vérifie qu'il correspond au créneau. L'ancienne route `/assignToCourse/{numRegis}/{numSkieur}` conserve son chemin mal nommé (le dernier segment désigne un cours) : elle accepte seulement une association déjà identique. Pour changer de cours, annuler puis réinscrire afin d'appliquer toutes les règles.

Les anciennes lignes sans date ne peuvent pas être situées dans une année : elles sont conservées dans les consultations historiques et exclues des rapports datés. Par prudence, une ligne historique non annulée bloque la réinscription du même skieur au même cours et numéro de semaine, quelle que soit l'année. Leur rattachement manuel à des créneaux nécessite une reprise de données explicite. La recherche `/registration/numWeeks/{instructorId}/{support}` conserve les semaines distinctes des inscriptions non annulées.

## Matériel et locations

| Méthode et chemin | Fonction / corps |
| --- | --- |
| `POST /equipment`, `GET /equipment` | Création / liste |
| `GET /equipment/{id}`, `PUT /equipment/{id}`, `DELETE /equipment/{id}` | Lecture / modification / suppression |
| `GET /equipment/available?from=&to=&type=` | Exemplaires disponibles sur la période |
| `POST /rentals` | `skierId`, `equipmentId`, `startDate`, `expectedEndDate` |
| `GET /rentals/{id}`, `GET /rentals/skier/{skierId}` | Lecture / historique |
| `GET /rentals?from=&to=` | Historique chevauchant la période |
| `PUT /rentals/{id}/activate` | Remise physique pendant la période réservée |
| `PUT /rentals/{id}/return` | `actualReturnDate` |
| `PUT /rentals/{id}/cancel` | Annulation d'une réservation |

Corps matériel :

```json
{"inventoryCode":"SKI-001","type":"SKIS","brand":"Exemple","size":"170 cm","dailyRate":25.00,"status":"AVAILABLE"}
```

Types : `SKIS`, `BOOTS`, `POLES`, `HELMET`. États : `AVAILABLE`, `RENTED`, `MAINTENANCE`, `OUT_OF_SERVICE`. `RENTED` est piloté par activation/retour, pas par le CRUD. Un exemplaire représente un objet physique ; code unique. Un équipement avec historique ne peut pas être supprimé ; utiliser `OUT_OF_SERVICE`. Sa modification est refusée tant qu'une réservation ou location active existe.

Transitions : `RESERVED → ACTIVE → RETURNED`, ou `RESERVED → CANCELLED`. Réserver dans le passé est interdit. Les contrôles sont sérialisés par verrou sur l'exemplaire. Maintenance/hors service interdisent réservation et activation. Une location active bloque toute réservation future jusqu'au retour réel, pour éviter de promettre un exemplaire non rendu ; les réservations futures préexistantes restent enregistrées mais leur activation échoue si l'exemplaire n'est pas rendu.

**Tarification :** le tarif journalier est figé sur la location. Par défaut, jours facturés = date de fin/retour moins date de début, début inclus et retour exclu, avec un minimum d'un jour (retour le jour même possible). Le prix prévu est recalculé au retour d'après la durée réelle, sans pénalité supplémentaire. La réservation exige une fin strictement postérieure au début ; le retour peut avoir lieu le jour de début, jamais dans le futur. `station.rental.include-return-day=true` ajoute le jour de retour à la durée facturée. Cette option concerne les montants, pas les frontières de chevauchement. Aucun montant de location n'est compté comme revenu encaissé sans paiement ; les paiements de cette version concernent les abonnements.

## Abonnements, paiements et rapports

Les routes d'abonnement existantes restent disponibles : création/modification/lecture, `/subscription/all/{typeSub}` et `/subscription/all/{date1}/{date2}`. Ajouts : `DELETE /subscription/delete/{id}` et `GET /subscription/{id}/status?asOf=`.

La fin est recalculée à partir du début à chaque création/modification : `ANNUAL` +1 an, `SEMESTRIEL` +6 mois, `MONTHLY` +1 mois calendaire (ajustement à la fin du mois). La date de fin est exclusive. Statuts calculés : `UPCOMING`, `ACTIVE`, `EXPIRED`. Un abonnement lié à un paiement est immuable ; sa suppression est refusée, comme celle d'un abonnement affecté à un skieur. Créer un nouvel abonnement pour un renouvellement. Les tâches toutes les 30 secondes sont supprimées ; le calcul historique de revenu récurrent est théorique, non utilisé dans les rapports d'encaissement.

| Méthode et chemin | Fonction / corps |
| --- | --- |
| `POST /payments` | `skierId`, `subscriptionId`, `amount`, `method`, `transactionReference` facultatif |
| `GET /payments/{id}` | Détail |
| `GET /payments/skier/{skierId}` | Historique par skieur |
| `GET /payments/subscription/{subscriptionId}` | Historique par abonnement |
| `PUT /payments/{id}/status` | `{"status":"PAID"}` ou `FAILED` |
| `POST /payments/{id}/refunds` | `{"amount":10.00,"reason":"Annulation partielle"}` |
| `GET /payments/{id}/refunds` | Historique des remboursements |
| `GET /payments/{id}/receipt` | Données JSON du reçu, statut, net encaissé, remboursements |
| `GET /reports/revenue?from=&to=` | Brut encaissé, remboursements, net et ventilation mensuelle |
| `GET /reports/subscriptions?asOf=` | Actifs/expirés/futurs/échéance sous 30 jours |
| `GET /reports/courses/occupancy?from=&to=` | Capacités cumulées et remplissage par cours collectif |
| `GET /reports/registrations?from=&to=` | Inscriptions actives par cours, année et semaine ISO |

Moyens : `CARD`, `CASH`, `BANK_TRANSFER`. Un paiement est créé `PENDING` pour l'abonnement actuellement affecté au skieur ; il passe à `PAID` ou `FAILED`. Une répétition du même statut est idempotente. Un paiement échoué n'est pas réactivé : enregistrer une nouvelle tentative. Les acomptes/paiements multiples sont autorisés sans imposer que chaque montant égale le prix de l'abonnement. Une référence fournie doit être unique ; un numéro de reçu UUID est généré côté serveur. Aucun appel bancaire, document PDF ni envoi de facture n'est effectué.

Les remboursements modifient uniquement les paiements encaissés : `PAID → PARTIALLY_REFUNDED → REFUNDED`, ou remboursement total direct. Le verrou paiement protège le cumul remboursé. Chaque remboursement conserve montant/date/motif ; ni paiement ni remboursement ne possède de route de suppression.

Le revenu brut inclut les paiements ayant un `paidAt` dans la période, même s'ils ont été remboursés plus tard. Les remboursements sont déduits à leur propre date, y compris si le paiement est antérieur à la période. Le net peut donc être négatif. Les mois sans mouvement sont omis ; les totaux d'une période vide sont zéro. Les prix théoriques d'abonnement ne sont pas utilisés. Les rapports de cours retiennent les créneaux commençant dans la période, excluent annulations et inscriptions sans créneau ; le remplissage est `inscriptions / somme des capacités × 100`, ou zéro sans capacité.

## Évolution du schéma

Les noms physiques suivent la stratégie de nommage Hibernate existante ; aucun script SQL n'a été exécuté.

| Élément | Impact |
| --- | --- |
| `instructor_availability` | Nouvelle table, FK moniteur, jour/heures et dates de validité |
| `planning_slot` | Nouvelle table, FK cours/moniteur, dates/heures, capacité, statut |
| `registration.planning_slot_id` | Nouvelle FK nullable pour conserver les inscriptions historiques |
| `registration.cancelled` | Booléen non nul avec défaut `false` pour les lignes existantes |
| `equipment` | Nouvelle table, code unique, tarif décimal, taille/type/statut |
| `rental` | Nouvelle table, FK skieur/exemplaire, dates, tarif figé, total et statut |
| `payment` | Nouvelle table, FK skieur/abonnement, montants décimaux, dates de création/encaissement, référence et reçu uniques |
| `refund` | Nouvelle table, FK paiement et historique du remboursement |
| `Skier.subscription` | Suppression de la cascade REMOVE au niveau JPA, sans effacement de données |

Les relations nouvelles sont portées par les `@ManyToOne` des enfants, sans collections inverses inutiles et sans cascade de suppression. Les relations skieurs–pistes, skieur–abonnement et moniteur–cours conservent leurs tables actuelles. Les boucles JSON sont évitées sur les relations inverses ; les réponses d'inscription exposent aussi `skierId` et `courseId` en lecture seule.

`Subscription.typeSub` conserve expressément le stockage ordinal existant : `ANNUAL=0`, `MONTHLY=1`, `SEMESTRIEL=2`. Ne pas changer l'ordre de cette enum sans reprise des données. Les anciens prix `Float`, `Course.timeSlot` et `Registration.numWeek` restent inchangés.

Avant d'utiliser une base historique, contrôler les dates/noms/type de cours et les associations déjà incohérentes. Les validations portent sur les nouvelles écritures ; aucune correction automatique des données historiques n'est réalisée. Le rapport d'abonnements signale explicitement les lignes sans dates plutôt que de les ignorer.

## Limites de vérification et dette technique

- Compilation, démarrage, OpenAPI généré, mappings Hibernate et concurrence MySQL non vérifiés à l'exécution ; aucun test exécuté.
- Les huit tests unitaires ajoutés ne remplacent pas les tests d'intégration des verrous, contraintes et reprises de schéma.
- Plusieurs recherches et rapports agrègent les lignes en mémoire. À fort volume, les remplacer par des requêtes filtrées/agrégées et ajouter les index adaptés ; seule la recherche des skieurs est paginée.
- Les contrôleurs historiques conservent leurs entités comme corps pour compatibilité. Les services copient les champs autorisés et utilisent des routes dédiées pour les associations ; les nouveaux domaines utilisent des DTO de requête. Certains détails de réponse demeurent des entités JPA sans boucle JSON.
- L'historique financier reste attaché au skieur et à l'abonnement. Le nom du skieur sur le reçu est lu à la consultation et n'est pas un instantané fiscal ; le numéro généré est un identifiant local, sans séquence fiscale juridictionnelle.
