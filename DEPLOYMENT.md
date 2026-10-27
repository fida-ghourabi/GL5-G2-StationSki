# Déploiement AWS / EKS

Ce dépôt fournit un pipeline Jenkins, des images Docker pour le backend Spring Boot et le frontend Angular, des manifests Kubernetes et une configuration Terraform qui crée un VPC (sous-réseaux publics/privés), un cluster EKS et une base RDS MySQL privée.

## Vue d'ensemble

| Composant | Technologie | Port | Fichiers |
|---|---|---|---|
| Backend | Spring Boot 2.7.5, Java 8 (cible), contexte `/stationSki` | 8087 | `backend/Dockerfile`, `k8s/backend.yaml` |
| Frontend | Angular 21 servi par Nginx non privilégié | 8080 (Service : 80) | `frontend/Dockerfile`, `frontend/nginx.conf`, `k8s/frontend.yaml` |
| Base de données | RDS MySQL `skidb` (privé) | 3306 | `infrastructure/terraform/main.tf` |
| CI/CD | Jenkins (pipeline déclaratif) | – | `Jenkinsfile` |

## Pré-requis Jenkins

L'agent Jenkins (conteneur Linux) doit fournir :

- **JDK 21** pour exécuter Maven (le projet compile avec la cible `java.version=1.8` ; les versions de Lombok et Byte Buddy sont relevées dans le `pom.xml` pour être compatibles avec ce JDK). L'image d'exécution du backend reste `eclipse-temurin:8-jre`.
- `docker` avec accès au Docker daemon (nécessaire à la fois pour construire les images **et pour les tests**, voir plus bas).
- `terraform` (>= 1.5), AWS CLI, `kubectl`, `jq`, `python3` et `curl`.
- Un accès Internet pour télécharger les dépendances Maven, les images Docker et les modules/providers Terraform.

Le frontend n'a pas besoin de Node sur l'agent : il est compilé dans le build Docker (`node:24-alpine`, `npm ci` puis `npm run build`).

L'étape « Vérification des outils » du pipeline contrôle la présence de `java`, `docker`, `terraform`, `aws`, `kubectl`, `jq` et `python3`.

### Credentials Jenkins

Créer ces credentials avant d'activer les étapes concernées :

| ID Jenkins | Type | Contenu |
|---|---|---|
| `aws-access-key-id` | Secret Text | Access key temporaire du Learner Lab |
| `aws-secret-access-key` | Secret Text | Secret key temporaire du Learner Lab |
| `aws-session-token` | Secret Text | Session token temporaire du Learner Lab |
| `dockerhub` | Username with password | Identifiants ou token Docker Hub |
| `sonar-token` | Secret Text | Token SonarQube |
| `nexus-creds` | Username with password | Compte Nexus autorisé à publier des snapshots |

Les clés AWS temporaires doivent être renouvelées dans Jenkins chaque fois que le Learner Lab les renouvelle. Ne jamais enregistrer dans Git les valeurs de ces clés, du token Docker, du mot de passe RDS ou du token Sonar.

## Paramètres du pipeline

| Paramètre | Défaut | Rôle |
|---|---|---|
| `RUN_SONAR` | `true` | Analyse du backend sur SonarQube |
| `PUBLISH_NEXUS` | `true` | Publication du JAR dans Nexus |
| `PUSH_IMAGES` | `true` | Publication des images sur Docker Hub |
| `PLAN_TERRAFORM` | `true` | Validation Terraform et affichage du plan (sans modification d'AWS) |
| `APPLY_TERRAFORM` | `false` | Création/mise à jour du VPC, EKS et RDS (coûts AWS) |
| `DEPLOY_TO_EKS` | `false` | Déploiement des images sur EKS |
| `AWS_REGION` | `us-east-1` | Région du Learner Lab |
| `EKS_CLUSTER_NAME` | `stationski-dev` | Nom du cluster EKS |
| `EKS_API_CIDR` | *(vide)* | CIDR public autorisé à joindre l'API EKS (IP de sortie de Jenkins en `/32`) |
| `DOCKERHUB_NAMESPACE` | `bennasserhajer` | Namespace Docker Hub propriétaire des images |
| `SONAR_URL` | `http://sonarqube:9000` | URL SonarQube joignable depuis Jenkins |
| `NEXUS_MAVEN_URL` | `http://nexus:8081/repository/maven-snapshots/` | Dépôt Maven snapshots Nexus |

Les valeurs initiales de `SONAR_URL` et `NEXUS_MAVEN_URL` supposent les alias Docker `sonarqube` et `nexus` ; les modifier si les alias ou ports réels diffèrent. Le namespace Docker Hub doit correspondre au compte lié au credential `dockerhub`.

> **Important :** `PLAN_TERRAFORM` est activé par défaut, mais `EKS_API_CIDR` est vide par défaut. L'étape Terraform échoue alors avec le message demandant de renseigner `EKS_API_CIDR`. Pour un build qui ne concerne que les tests, Sonar, Nexus et les images, **décocher `PLAN_TERRAFORM`** ; sinon renseigner `EKS_API_CIDR` (par exemple `203.0.113.10/32`).

## Exécution du pipeline

Configurer le dépôt comme un job Jenkins Pipeline ou Multibranch et sélectionner le `Jenkinsfile` à la racine.

### Étapes

1. **Checkout** : calcule le tag d'image `BUILD_NUMBER-<7 premiers caractères du commit>`.
2. **Vérification des outils**.
3. **Tests backend et couverture** : `./mvnw -B clean verify`.
4. **Analyse SonarQube** (si `RUN_SONAR`).
5. **Publication du JAR vers Nexus** (si `PUBLISH_NEXUS`).
6. **Construction des images** backend et frontend.
7. **Publication Docker Hub** (si `PUSH_IMAGES` ou `DEPLOY_TO_EKS`).
8. **Vérification connexion AWS** (si `PLAN_TERRAFORM`, `APPLY_TERRAFORM` ou `DEPLOY_TO_EKS`).
9. **Terraform : plan et provisionnement** (mêmes conditions).
10. **Déploiement sur EKS** (si `DEPLOY_TO_EKS`), suivi d'un smoke test HTTP sur l'API.

En fin de build, les rapports JUnit et JaCoCo sont archivés, `target/` est nettoyé et la session Docker est fermée.

### Restriction de branche

Le contrôle de branche (`feature/inscription`) est **actuellement désactivé** dans le `Jenkinsfile` (bloc commenté dans l'étape Terraform). Toute branche peut donc lancer Terraform, y compris `apply`, et le déploiement EKS : ne cocher `APPLY_TERRAFORM` et `DEPLOY_TO_EKS` que sciemment. Pour restreindre ces opérations, réactiver le contrôle `env.BRANCH_NAME` dans le `Jenkinsfile`.

### Tests backend

`./mvnw clean verify` compile, exécute les tests et applique le contrôle JaCoCo (couverture minimale de `RegistrationServicesImpl` : 80 % des lignes et 65 % des branches). Les rapports JUnit et le rapport HTML JaCoCo sont archivés.

Les tests s'exécutent sur une base H2 en mémoire via le profil `test` ; ils n'ont pas besoin d'un conteneur MySQL ni d'un accès Docker. La configuration MySQL de l'application normale n'est pas remplacée. Ils se répartissent ainsi :

- **Tests unitaires et MockMvc** (Mockito) : aucune infrastructure externe.
- **Tests d'intégration et de repository** : utilisent H2, créée en mémoire et détruite à la fin des tests.
- **Tests Mockito et MockMvc** : aucune base de données ni infrastructure externe.

### Publication

Le pipeline publie par défaut le JAR snapshot dans Nexus et les images taguées de manière unique sur Docker Hub. Désactiver `PUBLISH_NEXUS` ou `PUSH_IMAGES` au lancement si nécessaire. L'image `latest` n'est pas mise à jour automatiquement.

### Terraform et déploiement

1. **Plan seul** : `PLAN_TERRAFORM` activé. Jenkins exécute `init`, `fmt -check`, `validate` et `plan`, sans modifier AWS.
2. **Création/mise à jour de l'infrastructure** : activer `APPLY_TERRAFORM`. Jenkins affiche d'abord le plan puis l'applique.
3. **Déploiement** : activer `DEPLOY_TO_EKS`. Les images sont publiées avant le déploiement, puis Jenkins :
   - génère le kubeconfig et lit le mot de passe RDS dans Secrets Manager ;
   - crée le Secret `stationski-db` et le ConfigMap `stationski-backend-config` (`DB_URL`) ;
   - applique `k8s/backend.yaml` et `k8s/frontend.yaml` en substituant `IMAGE_PLACEHOLDER` ;
   - attend la fin des rollouts, récupère l'adresse du load balancer et exécute un smoke test sur `/stationSki/registration/skier/1` (HTTP 2xx ou 404 accepté).

Dans les cas 1 et 2, renseigner `EKS_API_CIDR` avec l'adresse IPv4 publique de sortie de Jenkins au format `x.x.x.x/32`. Le CIDR `0.0.0.0/0` est refusé par la validation Terraform.

Le déploiement lit les sorties Terraform (`rds_endpoint`, `rds_secret_arn`) : l'état Terraform doit donc être présent dans le workspace Jenkins.

## Infrastructure Terraform

- **VPC** `10.40.0.0/16` : 2 sous-réseaux publics et 2 privés sur 2 zones de disponibilité, un seul NAT Gateway.
- **EKS** 1.34 : cluster créé directement (sans module) avec `LabRole`, car le Learner Lab interdit la création de rôles IAM. Endpoint public restreint à `EKS_API_CIDR`. Un groupe de nœuds `t3.medium` (min 1, max 2, souhaité 1).
- **RDS MySQL** `db.t3.micro`, base `skidb`, utilisateur `ski_app`, non public, sans multi-AZ, sans snapshot final. Le mot de passe est généré et géré par AWS Secrets Manager. Le security group n'autorise le port 3306 que depuis le security group du cluster EKS.
- **Sorties** : `cluster_name`, `rds_endpoint`, `rds_secret_arn`, `vpc_id`.

## Configuration de l'application

- Le backend lit `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` (valeurs par défaut locales dans `application.properties`).
- Par défaut, `spring.jpa.hibernate.ddl-auto=validate`. Dans Kubernetes, `k8s/backend.yaml` force `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, car la base RDS est créée vide : Hibernate crée et synchronise le schéma au démarrage. Pour un environnement de production, remplacer ce mécanisme par des migrations versionnées (Flyway ou Liquibase).
- Le profil `local` (`application-local.properties`) active `ddl-auto=update` pour le développement local uniquement.
- Le frontend est servi par Nginx, qui transmet `/stationSki/` au Service interne `stationski-backend:8087`. Les autres routes retombent sur `index.html` (application Angular).

## Sécurité des conteneurs

- Backend : exécuté avec l'utilisateur `10001`, système de fichiers racine en lecture seule (volume `emptyDir` sur `/tmp`), toutes les capabilities supprimées.
- Frontend : image `nginx-unprivileged` (utilisateur `101`), mêmes restrictions.
- Les clés AWS ne sont jamais écrites dans les manifests ; les fichiers temporaires du déploiement sont supprimés en fin d'exécution.

## Monitoring

- Le backend embarque Actuator et le registre Micrometer Prometheus. Les probes Kubernetes utilisent `/stationSki/actuator/health` (startup), `/stationSki/actuator/health/readiness` et `/stationSki/actuator/health/liveness`. Ces probes sont activées dans le déploiement par la variable `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true`.
- Le Service backend porte les annotations `prometheus.io/*` (chemin `/stationSki/actuator/prometheus`, port 8087).
- **Limite actuelle :** `application.properties` ne définit pas `management.endpoints.web.exposure.include`. Avec Spring Boot 2.7, l'endpoint `prometheus` n'est donc pas exposé en HTTP tant que cette propriété n'est pas ajoutée (par exemple `management.endpoints.web.exposure.include=health,info,prometheus`).
- **Attention à l'exposition publique :** Nginx transmet tout `/stationSki/` au backend. Si l'endpoint `prometheus` (ou un autre endpoint Actuator) est exposé, il devient accessible via le load balancer public du frontend. Avant de l'activer, bloquer `/stationSki/actuator/` côté Nginx, ou n'y laisser passer que `health`.
- Le conteneur Prometheus existant hors du cluster ne peut pas résoudre le DNS `ClusterIP` d'EKS depuis le réseau Docker Jenkins. Pour scraper réellement l'application, Prometheus doit soit être déployé dans le cluster avec la découverte de services par annotations, soit disposer d'une connectivité privée vers le VPC. Grafana doit ensuite utiliser un datasource Prometheus joignable depuis son propre réseau.

## Coûts et limites du Learner Lab

Le cluster EKS, le NAT Gateway, le nœud EC2, le RDS, le stockage et le load balancer consomment le budget du Learner Lab même après la fin du pipeline. Le pipeline **ne détruit rien automatiquement**. Détruire explicitement les ressources lorsque le travail est terminé :

```sh
cd infrastructure/terraform
terraform destroy
```

La destruction nécessite le même état Terraform et des credentials AWS encore valides. Elle supprime aussi le RDS et ses données (la configuration saute le snapshot final) : exporter toute donnée à conserver avant de l'exécuter.

Le Service du frontend reçoit une adresse NLB publique en HTTP (pas de TLS ni de certificat dans ce dépôt). Pour un usage au-delà d'un exercice, ajouter un nom DNS, TLS/ACM et une stratégie d'authentification.

Le rôle `LabRole` doit autoriser la création des ressources EKS, VPC, NAT Gateway, RDS et Secrets Manager. Les comptes AWS Academy peuvent restreindre certaines actions, versions EKS ou tailles d'instance ; le pipeline échoue explicitement si une permission ou un quota manque.

## État Terraform

L'état Terraform est local au workspace Jenkins et exclu de Git. Utiliser un workspace Jenkins persistant et conserver l'état entre les builds ; un backend distant chiffré et partagé (S3 avec verrouillage) est recommandé avant tout usage collaboratif ou multi-agent.

## Fichiers DevOps du dépôt

- `Jenkinsfile` : tests, Sonar, Nexus, Docker Hub, Terraform et déploiement EKS conditionnels.
- `backend/Dockerfile` : image Java 8 (JRE) non-root du JAR compilé.
- `frontend/Dockerfile`, `frontend/nginx.conf`, `frontend/.dockerignore` : build Angular et Nginx non privilégié avec reverse proxy vers le backend.
- `infrastructure/terraform/` : VPC, EKS, RDS et sorties utilisées par Jenkins.
- `k8s/` : namespace, déploiement et Service privé du backend, déploiement et Service public du frontend.
