# Audit des contrats backend

Inventaire établi à partir des contrôleurs Java, entités, DTO, services et propriétés présents dans le dépôt. Il décrit les routes **dans le code**, sans prétendre qu’un serveur en cours d’exécution a été interrogé.

## Stack et configuration constatées

- Java cible : **1.8** ; Spring Boot **2.7.5**.
- Spring Web, Data JPA/Hibernate, connecteur MySQL **8.0.33**, Lombok, ModelMapper **2.3.5**, springdoc **1.6.9**, starter-test.
- Port : **8087** ; préfixe servlet : **`/stationSki`**.
- Aucun `@CrossOrigin`, configurateur CORS ou mécanisme d’authentification trouvé. Le proxy frontend évite le besoin d’une modification backend.
- GET retourne des listes/entités JSON, sauf `/skier/search` (page Spring) et le statut d’abonnement (chaîne). Les commandes historiques renvoient généralement `200`.
- Les DTO des nouvelles commandes utilisent des identifiants ; les réponses incluent souvent des entités imbriquées. Les identifiants diffèrent : `numSkier`, `numPiste`, `numCourse`, `numInstructor`, `numRegistration`, `numSub`, puis `id` pour les nouveaux domaines.

Toutes les routes ci-dessous sont préfixées par `/stationSki`. `?` indique les query parameters, les champs entre `{}` sont des segments du chemin. « Corps » signifie JSON. Les paramètres de recherche marqués « optionnels » peuvent être omis.

## Skieurs

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| GET | `/skier/all` | Aucun |
| GET | `/skier/get/{id}` | Identifiant positif |
| GET | `/skier/search` | `name`, `city`, `page=0`, `size=20` (max 100) |
| GET | `/skier/getSkiersBySubscription` | `typeSubscription` requis |
| POST | `/skier/add` | `firstName`, `lastName`, `dateOfBirth`, `city`, abonnement facultatif |
| POST | `/skier/addAndAssign/{numCourse}` | Corps skieur ; choisit le prochain créneau ouvert |
| PUT | `/skier/update` | Corps skieur avec `numSkier` ; champs personnels |
| PUT | `/skier/assignToSub/{numSkier}/{numSub}` | Sans corps utile |
| PUT | `/skier/assignToPiste/{numSkier}/{numPiste}` | Sans corps utile |
| DELETE | `/skier/delete/{id}` | Refus si historique |

Le frontend crée le dossier personnel, puis propose les affectations explicites. La route combinée de création/inscription existe mais n’est pas nécessaire à ce parcours.

## Pistes

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| GET | `/piste/all` | Aucun |
| GET | `/piste/get/{id}` | Identifiant |
| GET | `/piste/search` | `color` optionnel |
| GET | `/piste/{id}/skiers` | Skieurs affectés |
| POST | `/piste/add` | `namePiste`, `color`, `length`, `slope` |
| PUT | `/piste/update` | Mêmes champs + `numPiste` |
| DELETE | `/piste/delete/{id}` | Refus si skieurs affectés |

## Cours

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| GET | `/course/all` | Aucun |
| GET | `/course/get/{id}` | Identifiant |
| GET | `/course/search` | `type`, `support`, `level` optionnels |
| GET | `/course/{id}/registrations` | Historique |
| GET | `/course/{id}/participants/count` | Nombre de skieurs distincts, inscriptions non annulées |
| GET | `/course/{id}/instructors` | Liste du moniteur associé |
| POST | `/course/add` | `level`, `typeCourse`, `support`, `price`, `timeSlot` historique |
| PUT | `/course/update` | Mêmes champs + `numCourse` |
| DELETE | `/course/delete/{id}` | Sans associations ni historique |

Le frontend transmet `timeSlot=0` à la création et conserve sa valeur lors des modifications ; les horaires réels sont ceux des créneaux.

## Moniteurs

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| GET | `/instructor/all` | Aucun |
| GET | `/instructor/get/{id}` | Identifiant |
| GET | `/instructor/search` | `name` optionnel |
| GET | `/instructor/{id}/courses` | Cours associés |
| POST | `/instructor/add` | `firstName`, `lastName`, `dateOfHire` |
| PUT | `/instructor/update` | Mêmes champs + `numInstructor` |
| PUT | `/instructor/addAndAssignToCourse/{numCourse}` | Nouveau moniteur |
| PUT | `/instructor/{id}/courses/{courseId}` | Associer un cours existant |
| DELETE | `/instructor/{id}/courses/{courseId}` | Retirer l’association sans créneaux futurs actifs |
| DELETE | `/instructor/delete/{id}` | Sans cours, créneaux ni disponibilités |

## Inscriptions

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| GET | `/registration` | `from`, `to` datetime requis, `courseId` optionnel |
| GET | `/registration/skier/{skierId}` | Historique complet, anciennes lignes incluses |
| POST | `/registration/skier/{skierId}/slot/{slotId}` | Sans corps utile |
| DELETE | `/registration/{id}` | Annulation logique |
| GET | `/registration/numWeeks/{numInstructor}/{support}` | Semaines distinctes non annulées |
| PUT | `/registration/addAndAssignToSkier/{numSkieur}` | Corps `planningSlot: {id}` obligatoire |
| PUT | `/registration/addAndAssignToSkierAndCourse/{numSkieur}/{numCourse}` | Même corps ; vérifie le cours du créneau |
| PUT | `/registration/assignToCourse/{numRegis}/{numSkieur}` | Le dernier segment est en réalité le cours ; association identique seulement |

Les anciennes routes partielles ne sont pas utilisées pour les nouvelles inscriptions. Le frontend utilise la route explicite skieur/créneau. Filtre semaine effectué localement sur les résultats renvoyés. Pas de route de détail individuel : le frontend retrouve l’inscription dans l’historique du skieur indiqué dans l’URL.

## Planning

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| POST | `/planning/availability/{instructorId}` | `dayOfWeek`, `startTime`, `endTime`, `validFrom`, `validUntil` |
| GET | `/planning/instructors/{id}/availability` | Disponibilités |
| POST | `/planning/slots` | `courseId`, `instructorId`, `startTime`, `endTime`, `maxCapacity` |
| GET | `/planning/slots/{id}` | Créneau |
| GET | `/planning/slots` | `from`, `to` requis ; `courseId`, `instructorId` optionnels |
| GET | `/planning/slots/available` | `from`, `to` requis ; `courseId` optionnel |
| PUT | `/planning/slots/{id}` | Même corps qu’à la création |
| DELETE | `/planning/slots/{id}` | Annule le créneau et ses inscriptions |

Le filtre moniteur des créneaux disponibles est appliqué localement car la route `/available` ne le prend pas. Capacités connues : 1 en individuel, jusqu’à 6 en collectif. L’interface ne déduit pas un nombre de places restantes à partir du simple statut `OPEN`.

## Matériel et locations

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| GET | `/equipment` | Inventaire complet |
| GET | `/equipment/{id}` | Exemplaire |
| POST | `/equipment` | `inventoryCode`, `type`, `brand`, `size`, `dailyRate`, `status` |
| PUT | `/equipment/{id}` | Mêmes champs |
| DELETE | `/equipment/{id}` | Sans historique |
| GET | `/equipment/available` | `from`, `to` dates requis ; `type` optionnel |
| POST | `/rentals` | `skierId`, `equipmentId`, `startDate`, `expectedEndDate` |
| GET | `/rentals/{id}` | Location |
| GET | `/rentals/skier/{skierId}` | Historique complet |
| GET | `/rentals` | `from`, `to` dates requis |
| PUT | `/rentals/{id}/activate` | Sans corps utile |
| PUT | `/rentals/{id}/return` | `actualReturnDate` |
| PUT | `/rentals/{id}/cancel` | Sans corps utile |

Les filtres d’inventaire taille/état et le filtre de statut des locations sont locaux. Les disponibilités, chevauchements, tarifs et montants de location viennent du serveur. Le frontend ne calcule pas un devis supposant une configuration de facturation inconnue.

## Abonnements

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| POST | `/subscription/add` | `startDate`, `price`, `typeSub` |
| PUT | `/subscription/update` | Mêmes champs + `numSub` |
| GET | `/subscription/get/{id}` | Abonnement |
| GET | `/subscription/all/{typeSub}` | `ANNUAL`, `MONTHLY`, `SEMESTRIEL` |
| GET | `/subscription/all/{date1}/{date2}` | Début de l’abonnement entre deux dates incluses |
| GET | `/subscription/{id}/status` | `asOf` date requise ; réponse chaîne |
| DELETE | `/subscription/delete/{id}` | Sans skieur affecté ni paiement |

La liste globale est composée des trois appels par type. Les statuts de liste sont calculés à partir des dates, selon la même règle de fin exclusive, avec une catégorie visuelle « échéance proche » sous 30 jours ; pas de requête de statut par ligne. Le filtre « actifs » inclut les échéances proches. Un abonnement ayant des paiements est immuable côté backend.

## Paiements et rapports

| Méthode | Chemin | Paramètres / corps |
| --- | --- | --- |
| POST | `/payments` | `skierId`, `subscriptionId`, `amount`, `method`, `transactionReference` facultative |
| GET | `/payments/{id}` | Paiement |
| GET | `/payments/skier/{skierId}` | Historique |
| GET | `/payments/subscription/{subscriptionId}` | Historique |
| PUT | `/payments/{id}/status` | `status` : `PAID` ou `FAILED` depuis `PENDING` |
| POST | `/payments/{id}/refunds` | `amount`, `reason` |
| GET | `/payments/{id}/refunds` | Historique des remboursements |
| GET | `/payments/{id}/receipt` | DTO du reçu |
| GET | `/reports/revenue` | `from`, `to` datetime requis |
| GET | `/reports/subscriptions` | `asOf` date requise |
| GET | `/reports/courses/occupancy` | `from`, `to` datetime requis |
| GET | `/reports/registrations` | `from`, `to` datetime requis |

Paiement enregistré en attente, encaissement confirmé par une action distincte. Remboursement avec montant restant et confirmation explicite. Aucun numéro de carte n’est demandé, stocké ou transmis. Le revenu est celui du rapport backend, pas une somme reconstruite des prix d’abonnement ou des locations.

## Fonctionnalités accessibles et fonctions sans route

Les onze rubriques métier sont alimentées par des routes existantes, y compris planning, locations, paiements et rapports (elles ne sont plus de simples fonctionnalités futures dans cet état du dépôt).

Fonctions sans route, donc non implémentées comme opérations : authentification/rôles, météo, remontées mécaniques, ouverture/fermeture des pistes, modification/suppression des disponibilités, dissociation skieur–piste, suppression de paiements/remboursements, paiement bancaire externe, PDF fiscal, paiement des locations. Les formulaires et indicateurs associés ne sont pas simulés.

## Points de vigilance

1. **Disponibilité du serveur** : les anciennes difficultés de démarrage/Swagger ne prouvent pas que les routes répondent actuellement. La compilation frontend ne vérifie pas la connexion MySQL ni les réponses HTTP du backend.
2. **CORS** : passage par le proxy `/stationSki/**` en développement, reverse proxy en production.
3. **Périodes obligatoires** : pas de liste globale non bornée des inscriptions et locations ; bornes précisées dans les écrans.
4. **Historique d’inscriptions** : `planningSlot` nullable, `skier`/`course` ignorés en JSON ; utilisation de `skierId` et `courseId`. Les anciens enregistrements sans date ne sont pas inventés dans le calendrier.
5. **Association moniteur–cours** : un cours a un seul moniteur ; le planning exige l’association avant création et une disponibilité couvrant le créneau.
6. **Suppressions et modifications** : les protections d’historique produisent des `409` ; elles restent affichées plutôt que traitées comme des succès.
7. **Monnaie** : absente du contrat backend. Unité d’affichage configurable, aucune conversion.
8. **Volume** : seule la recherche skieurs est paginée par le serveur. Les autres filtres locaux et lookups nécessitent les listes retournées par l’API ; à faire évoluer côté API pour les grands volumes.
9. **Compatibilité Java/backend** : aucun fichier Java, propriété serveur, dépendance Maven ou schéma n’est changé par cette implémentation frontend.
