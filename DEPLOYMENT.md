# Déploiement AWS / EKS

Ce dépôt fournit un pipeline Jenkins, des images Docker pour le backend Spring Boot et le frontend Angular, des manifests Kubernetes et une configuration Terraform pour créer un VPC privé/public, EKS et une base RDS MySQL privée.

## Pré-requis Jenkins

L’agent Jenkins doit avoir accès au Docker daemon et contenir Java 8/11, `docker`, `terraform` (>= 1.5), AWS CLI, `kubectl`, `jq` et Python 3. Les outils Docker, Terraform et AWS doivent pouvoir joindre Internet pour télécharger les images et modules requis.

Créer ces credentials dans Jenkins avant d’activer les étapes concernées :

| ID Jenkins | Type | Contenu |
|---|---|---|
| `aws-access-key-id` | Secret Text | Access key temporaire du Learner Lab |
| `aws-secret-access-key` | Secret Text | Secret key temporaire du Learner Lab |
| `aws-session-token` | Secret Text | Session token temporaire du Learner Lab |
| `dockerhub` | Username with password | Identifiants/token Docker Hub |
| `sonar-token` | Secret Text | Token SonarQube |
| `nexus-creds` | Username with password | Compte Nexus autorisé à publier des snapshots |

Les clés AWS temporaires doivent être renouvelées dans Jenkins chaque fois que le Learner Lab les renouvelle. Ne pas enregistrer les valeurs de ces clés, du token Docker, du mot de passe RDS ou du token Sonar dans Git.

Les paramètres `SONAR_URL` et `NEXUS_MAVEN_URL` du pipeline doivent être joignables depuis le conteneur Jenkins. Leurs valeurs initiales supposent les alias Docker `sonarqube` et `nexus`; les modifier dans Jenkins si les alias/ports réels diffèrent. Le dépôt Nexus par défaut est `maven-snapshots`. Le namespace Docker Hub initial est `bennasserhajer`; le remplacer par le namespace possédé par le compte lié à `dockerhub`.

## Exécution du pipeline

Configurer le dépôt comme un job Jenkins Pipeline/Multibranch et sélectionner le `Jenkinsfile` à la racine. Le pipeline est orienté Linux (conteneur Jenkins) et limite la création AWS ainsi que le déploiement à `feature/inscription`.

Les tests backend sont exécutés par `./mvnw clean verify`. Cela compile, exécute les tests unitaires, tests MockMvc et tests de repository sur H2, et applique le contrôle JaCoCo. Le frontend est compilé lors de sa construction Docker (`npm ci` et `npm run build`). Les rapports JUnit et HTML JaCoCo sont archivés dans Jenkins.

Le pipeline publie par défaut le JAR snapshot à Nexus et les images taguées de manière unique à Docker Hub. Désactiver `PUBLISH_NEXUS` ou `PUSH_IMAGES` au lancement si nécessaire. L’image `latest` n’est pas mise à jour automatiquement.

Le plan Terraform est activé par défaut : Jenkins vérifie la configuration et affiche les changements prévus, sans modifier AWS. `APPLY_TERRAFORM` et `DEPLOY_TO_EKS` restent désactivés par défaut :

1. Pour voir uniquement ce que Terraform prévoit, activer `PLAN_TERRAFORM` : Jenkins valide la configuration puis affiche le plan, sans appliquer les changements ni déployer l’application. Pour créer ou mettre à jour l’infrastructure, activer `APPLY_TERRAFORM` : Jenkins affiche d’abord le plan puis l’applique. Dans les deux cas, renseigner `EKS_API_CIDR` avec l’adresse IPv4 publique de sortie de Jenkins, au format CIDR précis (par exemple `203.0.113.10/32`).
2. Pour déployer, activer `DEPLOY_TO_EKS`. Les images sont publiées avant le déploiement. Il faut que l’état Terraform de l’infrastructure soit toujours présent dans le workspace Jenkins `feature/inscription`.

Terraform crée une base RDS `skidb` vide et demande à AWS de générer/gérer le mot de passe principal dans Secrets Manager. Au déploiement, Jenkins lit ce secret en mémoire, le transmet dans un Secret Kubernetes temporaire et nettoie ses fichiers temporaires. Les clés AWS ne sont jamais écrites dans les manifests. L’application crée/synchronise actuellement le schéma initial avec Hibernate `ddl-auto=update` dans Kubernetes ; pour un environnement de production, remplacer ce mécanisme par des migrations versionnées avant le déploiement.

## Attention aux coûts et limites du Learner Lab

Le cluster EKS, le NAT Gateway, le nœud EC2, le RDS, le stockage et le load balancer peuvent consommer le budget Learner Lab même après la fin du pipeline. Le pipeline **ne détruit rien automatiquement**. Surveiller les coûts et détruire explicitement les ressources lorsque le travail est terminé :

```sh
cd infrastructure/terraform
terraform destroy
```

La destruction nécessite le même état Terraform et des credentials AWS encore valides. Elle supprime aussi le RDS et ses données (la configuration de développement saute le snapshot final) : exporter toute donnée à conserver avant de l’exécuter.

Le Service du frontend reçoit une adresse NLB publique en HTTP (pas de TLS/certificat configuré dans ce dépôt). Pour un service exposé au-delà d’un exercice, ajouter un nom DNS, TLS/ACM et une stratégie d’authentification avant toute mise en production.

Le rôle IAM du Learner Lab doit autoriser la création des rôles EKS, VPC, NAT Gateway, RDS et des secrets associés. Les comptes AWS Academy peuvent restreindre certaines de ces actions ou tailles d’instance ; le pipeline échouera explicitement si une permission ou un quota manque. EKS, RDS et Terraform n’ont pas été créés dans le compte AWS de l’utilisateur pendant la préparation des fichiers.

## Monitoring

Le backend expose ses métriques Micrometer à `/stationSki/actuator/prometheus` et l’état de santé aux chemins Actuator utilisés par les probes Kubernetes. Le service backend porte les annotations standard `prometheus.io/*`.

Le conteneur Prometheus existant hors du cluster ne peut pas résoudre le DNS `ClusterIP` d’EKS depuis le réseau Docker Jenkins. Pour scraper réellement l’application, Prometheus doit soit être déployé dans le cluster avec la découverte des services par annotations, soit avoir une connectivité privée au VPC et à ses cibles. L’endpoint de métriques n’est volontairement pas publié sur l’Internet via le load balancer du frontend. Grafana doit ensuite utiliser le datasource Prometheus joignable depuis son propre réseau.

## Composants ajoutés

- `backend/Dockerfile` : image Java non-root du JAR compilé.
- `frontend/Dockerfile`, `frontend/nginx.conf` et `frontend/.dockerignore` : build Angular reproductible et Nginx non privilégié qui transmet `/stationSki/` au service backend interne.
- `infrastructure/terraform/` : VPC, EKS, RDS privé avec secret principal géré par AWS et sorties utilisées par Jenkins.
- `k8s/` : namespace, déploiement/service privés du backend et déploiement/service public du frontend.
- `Jenkinsfile` : tests, analyse SonarQube, publication Nexus/Docker Hub, provisionnement Terraform conditionnel et déploiement EKS conditionnel.

L’état Terraform est local au workspace Jenkins et exclu de Git. Utiliser un workspace Jenkins persistant et conserver l’état entre les builds de `feature/inscription`; un backend distant chiffré/partagé (S3 avec verrouillage) est recommandé avant un usage collaboratif ou multi-agent.