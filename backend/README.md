# Gestion de station de ski

Backend Java 8 / Spring Boot 2.7.5, Spring Web, JPA/Hibernate, MySQL, Lombok et springdoc 1.6.9. Les paiements sont enregistrés localement. Aucune dépendance supplémentaire n'est nécessaire.

Le connecteur MySQL existant est fixé à `8.0.33` (compatible Java 8) pour ne pas dépendre de la gestion de ses coordonnées Maven selon la version du BOM. JUnit Jupiter et Mockito utilisent les versions cohérentes fournies par `spring-boot-starter-test`.

## Lancer en développement

Prérequis : JDK 8 ou 11, Maven 3.6+ et MySQL 8. Préparer une base `skidb` et un compte MySQL autorisé à l'utiliser. Le projet ne crée plus automatiquement la base. Exécuter les commandes Maven depuis `backend/`.

Configurer les variables d'environnement, par exemple dans PowerShell :

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/skidb?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8'
$env:DB_USERNAME = 'ski_app'
$env:DB_PASSWORD = '<mot-de-passe-local>'
mvn spring-boot:run '-Dspring-boot.run.profiles=local'
```

Le profil `local` utilise `ddl-auto=update` : **son lancement synchronise le schéma de la base choisie**. Hors de ce profil, `ddl-auto=validate` vérifie seulement le schéma ; préparer les changements décrits dans [la référence métier](docs/backend.md#évolution-du-schéma) avant le lancement. Aucun changement de base n'a été exécuté lors de cette implémentation.

Si Maven n'est pas installé, `mvnw.cmd` (Windows) ou `./mvnw` peut le remplacer. Au premier lancement, le wrapper télécharge Maven s'il n'est pas déjà en cache. Les commandes de construction peuvent également télécharger les dépendances manquantes.

Ne pas enregistrer de vrais identifiants dans les fichiers suivis. Les propriétés acceptent `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` ; les valeurs par défaut ciblent MySQL local sans mot de passe.

- API : `http://localhost:8087/stationSki`
- Swagger : `http://localhost:8087/stationSki/swagger-ui/index.html`
- OpenAPI : `http://localhost:8087/stationSki/v3/api-docs`
- Groupe OpenAPI : `http://localhost:8087/stationSki/v3/api-docs/SKI%20STATION%20Management%20API`

Les routes annotées sont incluses dans le groupe Swagger ; l'exclusion globale qui les masquait a été supprimée. Leur génération à l'exécution reste à vérifier.

## Architecture

```text
controllers/  →  services/ (I…Services → …ServicesImpl)  →  repositories/  →  entities/
                      ↕
                    dto/
```

Les huit domaines conservés suivent la même structure : skieurs, pistes, cours, moniteurs, inscriptions, planning, abonnements et paiements. Chaque service possède son interface et une implémentation `@Service`, avec injection par constructeur (`@RequiredArgsConstructor`, dépendances `final`). Les contrôleurs et les dépendances interservices utilisent les interfaces, jamais les classes `…ServicesImpl`.

Les transactions et règles métier restent dans les implémentations, avec lectures `readOnly` et verrous conservés pour les écritures sensibles. `TimeRanges` centralise les comparaisons de créneaux, sans dépendance entre implémentations. Les disponibilités appartiennent au planning ; les remboursements et reçus appartiennent aux paiements et ne nécessitent pas des services CRUD artificiels séparés.

`ApiExceptionHandler` fournit les erreurs JSON communes. Les requêtes planning/paiement utilisent des DTO avec identifiants ; `PaymentReceipt` conserve le contrat JSON du reçu. Les noms des repositories existants sont conservés : ce sont tous des interfaces Spring Data JPA.

## Fonctionnalités retirées

Les rapports et le matériel/location sont retirés du code backend et de l’interface Angular. Les routes `/reports/**`, `/equipment/**` et `/rentals/**` ne sont plus exposées. Leurs services, repositories, entités et DTO dédiés ne sont plus présents ; les paiements, remboursements et reçus restent disponibles.

**Aucune table ni donnée n’est supprimée.** Sur une base historique, les anciennes tables `equipment` et `rental` peuvent subsister. Leurs clés étrangères peuvent encore empêcher la suppression d’un skieur : l’API renverra alors un conflit `409`. Aucune migration de suppression ni cascade n’est ajoutée. Redémarrer le serveur sur les nouvelles classes après compilation.

Voir [la référence backend](docs/backend.md) pour les routes, hypothèses, transitions et changements de schéma.

## Vérifier le projet

Les tests n'ont pas été exécutés. Pour les lancer explicitement dans votre environnement :

```powershell
# Tests métier et structure isolés, sans base
mvn '-Dtest=BusinessRulesTest,ServiceStructureTest' test
# Suite complète : la base H2 de test est créée automatiquement
mvn test
# Compilation seule, sans compilation ni exécution des tests
mvn '-Dmaven.test.skip=true' compile
```

`BusinessRulesTest` couvre la capacité, un conflit moniteur, les transitions financières, le plafond de remboursement, le reçu, les dates d'abonnement, les créneaux adjacents et l’orchestration via les interfaces. Les tests des fonctionnalités retirées ont été supprimés. H2 valide les règles et mappings sous tests, mais ne garantit pas une équivalence complète avec MySQL pour les transactions/verrous concurrents.

`ServiceStructureTest` vérifie que les huit domaines ont un contrat de service, une implémentation et un contrôleur dépendant du contrat, ainsi que des dépendances `final` injectées par constructeur. Ces tests ont été préparés/compilés, pas exécutés.

Vérification du retrait/harmonisation : `mvn -o -DskipTests test-compile` réussit sous JDK 11, sans exécution des tests ni connexion MySQL. `clean` n’était pas disponible hors ligne (plugin non présent en cache) ; la compilation a été effectuée sans cette phase. Aucun test Spring/HTTP ou parcours de bout en bout n’a été exécuté.
