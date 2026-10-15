# Gestion de station de ski

Backend Java 8 / Spring Boot 2.7.5, Spring Web, JPA/Hibernate, MySQL, Lombok et springdoc 1.6.9. Les paiements sont enregistrés localement. Aucune dépendance supplémentaire n'est nécessaire.

Le connecteur MySQL existant est fixé à `8.0.33` (compatible Java 8) pour ne pas dépendre de la gestion de ses coordonnées Maven selon la version du BOM. JUnit Jupiter et Mockito utilisent les versions cohérentes fournies par `spring-boot-starter-test`.

## Lancer en développement

Prérequis : JDK 8 (ou JDK compatible avec cette version de Lombok), Maven 3.6+ et MySQL 8. Préparer une base `skidb` et un compte MySQL autorisé à l'utiliser. Le projet ne crée plus automatiquement la base.

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
controllers/  →  services/ + interfaces existantes  →  repositories/  →  entities/
                      ↕
                    dto/
```

Les transactions et règles métier restent dans les services. `ApiExceptionHandler` fournit les erreurs JSON communes. Les requêtes des nouveaux domaines utilisent des DTO avec identifiants ; les rapports et reçus ont des réponses typées.

Voir [la référence backend](docs/backend.md) pour les routes, hypothèses, transitions et changements de schéma.

## Vérifier le projet

Les tests n'ont pas été exécutés. Pour les lancer explicitement dans votre environnement :

```powershell
# Tests métier isolés, sans base
mvn '-Dtest=BusinessRulesTest' test
# Suite complète : le test contextLoads existant exige une base et un schéma compatibles
mvn test
# Compilation seule, sans compilation ni exécution des tests
mvn '-Dmaven.test.skip=true' compile
```

`BusinessRulesTest` couvre la capacité, un conflit moniteur, la réservation d'un exemplaire encore actif, la facturation incluant un jour bissextile, les transitions financières, le plafond de remboursement, la ventilation temporelle du revenu et les dates d'abonnement. Les transactions/verrous nécessitent encore des tests d'intégration concurrents sur MySQL ; les mocks ne prouvent pas leur comportement réel.

Lors de l'implémentation, seule une revue statique a été possible : Maven n'était pas disponible, le cache du wrapper était absent et seul Java 24 était installé. Aucune compilation, exécution Spring, requête HTTP ou connexion MySQL n'a été effectuée.
