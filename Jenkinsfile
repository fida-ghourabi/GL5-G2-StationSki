// pipeline {
//     agent any

//     options {
//         timestamps()
//         timeout(time: 120, unit: 'MINUTES')
//         buildDiscarder(logRotator(numToKeepStr: '10'))
//         disableConcurrentBuilds()
//     }

//     parameters {
//         booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyser le backend sur SonarQube.')
//         booleanParam(name: 'PUBLISH_NEXUS', defaultValue: true, description: 'Publier le JAR dans le dépôt Maven Nexus.')
//         booleanParam(name: 'PUSH_IMAGES', defaultValue: true, description: 'Publier les images backend et frontend sur Docker Hub.')
//         booleanParam(name: 'PLAN_TERRAFORM', defaultValue: true, description: 'Vérifier Terraform et afficher le plan sans appliquer ni déployer.')
//         booleanParam(name: 'APPLY_TERRAFORM', defaultValue: true, description: 'Créer ou mettre à jour VPC, EKS et RDS (coûts AWS).')
//         booleanParam(name: 'DEPLOY_TO_EKS', defaultValue: true, description: 'Déployer les images sur le cluster EKS.')
//         booleanParam(name: 'DESTROY_TERRAFORM', defaultValue: true, description: 'DANGER : supprime VPC, EKS et RDS (données perdues) à la fin du build, après le apply.')
//         booleanParam(name: 'DEPLOY_MONITORING', defaultValue: true, description: 'Installer Prometheus et Grafana (Helm) apres le deploiement sur EKS.')
//         string(name: 'GIT_REVISION', defaultValue: 'feature/hajer-iben-nasr', description: 'Branche Git suivie par ArgoCD (les fichiers k8s/app doivent etre pousses sur cette branche).')
//         string(name: 'TEST_MINUTES', defaultValue: '15', description: 'Duree de la pause pour tester (Grafana, ArgoCD, application) avant la destruction automatique.')
//         string(name: 'GIT_REPO_URL', defaultValue: 'https://github.com/fida-ghourabi/GL5-G2-StationSki.git', description: 'URL HTTPS du depot GitHub pour ArgoCD (laisser vide = URL du checkout Jenkins).')
//         string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'Région du Learner Lab.')
//         string(name: 'EKS_CLUSTER_NAME', defaultValue: 'stationski-cluster', description: 'Nom du cluster Terraform/EKS.')
//         string(name: 'EKS_API_CIDR', defaultValue: '', description: 'CIDR public restreint pour Jenkins vers l’API EKS, par exemple son IP sortante suivie de /32.')
//         string(name: 'DOCKERHUB_NAMESPACE', defaultValue: 'bennasserhajer', description: 'Namespace Docker Hub propriétaire des images.')
//         string(name: 'SONAR_URL', defaultValue: 'http://sonarqube:9000', description: 'URL SonarQube joignable depuis Jenkins.')
//         string(name: 'NEXUS_MAVEN_URL', defaultValue: 'http://nexus:8081/repository/maven-snapshots/', description: 'URL du dépôt Maven snapshots Nexus.')
//     }

//     environment {
//         DOCKER_CREDENTIALS    = 'dockerhub'
//         SONAR_CREDENTIALS     = 'sonar-token'
//         NEXUS_CREDENTIALS     = 'nexus-creds'
//         AWS_ACCESS_KEY_ID_CREDENTIALS     = 'aws-access-key-id'
//         AWS_SECRET_ACCESS_KEY_CREDENTIALS = 'aws-secret-access-key'
//         AWS_SESSION_TOKEN_CREDENTIALS     = 'aws-session-token'
//         IMAGE_TAG = 'pending'
//         BACKEND_IMAGE = "${params.DOCKERHUB_NAMESPACE}/stationski-backend"
//         FRONTEND_IMAGE = "${params.DOCKERHUB_NAMESPACE}/stationski-frontend"
//     }

//     stages {
//         stage('Checkout') {
//             steps {
//                 checkout scm
//                 script {
//                     env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
//                 }
//             }
//         }

//         stage('Vérification des outils') {
//             steps {
//                 sh '''
//                     set -eu
//                     java -version
//                     chmod +x backend/mvnw
//                     ./backend/mvnw -version
//                     command -v docker
//                     command -v terraform
//                     command -v aws
//                     command -v kubectl
//                     command -v jq
//                     command -v python3
//                 '''
//             }
//         }

//         stage('Tests backend et couverture') {
//             steps {
//                 dir('backend') {
//                     sh 'chmod +x mvnw && ./mvnw -B clean verify'
//                 }
//             }
//         }

//         stage('Analyse SonarQube') {
//             when { expression { params.RUN_SONAR } }
//             steps {
//                 withCredentials([string(credentialsId: env.SONAR_CREDENTIALS, variable: 'SONAR_TOKEN')]) {
//                     dir('backend') {
//                         sh '''
//                             ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
//                               -Dsonar.projectKey=stationski-inscription \
//                               -Dsonar.host.url="$SONAR_URL" \
//                               -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
//                         '''
//                     }
//                 }
//             }
//         }

//         stage('Publication du JAR vers Nexus') {
//             when { expression { params.PUBLISH_NEXUS } }
//             steps {
//                 withCredentials([usernamePassword(credentialsId: env.NEXUS_CREDENTIALS,
//                                                   usernameVariable: 'NEXUS_USER',
//                                                   passwordVariable: 'NEXUS_PASSWORD')]) {
//                     dir('backend') {
//                         sh '''
//                             set +x
//                             chmod +x mvnw
//                             settings_file="$(mktemp)"
//                             trap 'rm -f "$settings_file"' EXIT
//                             python3 -c 'import os, xml.sax.saxutils; print("<settings><servers><server><id>stationski-nexus</id><username>" + xml.sax.saxutils.escape(os.environ["NEXUS_USER"]) + "</username><password>" + xml.sax.saxutils.escape(os.environ["NEXUS_PASSWORD"]) + "</password></server></servers></settings>")' > "$settings_file"
//                             ./mvnw -B -s "$settings_file" org.apache.maven.plugins:maven-deploy-plugin:3.1.3:deploy-file \
//                               -DrepositoryId=stationski-nexus \
//                               -Durl="$NEXUS_MAVEN_URL" \
//                               -Dfile=target/gestion-station-skii-0.0.1-SNAPSHOT.jar \
//                               -DpomFile=pom.xml \
//                               -DgeneratePom=false
//                         '''
//                     }
//                 }
//             }
//         }

//         stage('Construction des images') {
//             steps {
//                 sh '''
//                     docker build -t "$BACKEND_IMAGE:$IMAGE_TAG" backend
//                     docker build -t "$FRONTEND_IMAGE:$IMAGE_TAG" frontend
//                 '''
//             }
//         }

//         stage('Publication Docker Hub') {
//             when { expression { params.PUSH_IMAGES || params.DEPLOY_TO_EKS } }
//             steps {
//                 withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS,
//                                                   usernameVariable: 'DOCKER_USER',
//                                                   passwordVariable: 'DOCKER_PASSWORD')]) {
//                     sh '''
//                         set +x
//                         printf '%s' "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USER" --password-stdin
//                         docker push "$BACKEND_IMAGE:$IMAGE_TAG"
//                         docker push "$FRONTEND_IMAGE:$IMAGE_TAG"
//                     '''
//                 }
//             }
//         }

//         stage('Vérification connexion AWS') {
//             when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS || params.DESTROY_TERRAFORM } }
//             steps {
//                 withCredentials([
//                     string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
//                     string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
//                     string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
//                 ]) {
//                     withEnv(["AWS_REGION=${params.AWS_REGION}", "AWS_DEFAULT_REGION=${params.AWS_REGION}"]) {
//                         sh '''
//                             set +x
//                             echo 'Vérification de la connexion AWS et de l’identité utilisée par Jenkins...'
//                             aws sts get-caller-identity --output table
//                         '''
//                     }
//                 }
//             }
//         }

//         stage('Terraform : plan et provisionnement') {
//             when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS } }
//             steps {
//                 script {
//                     if (!params.EKS_API_CIDR?.trim()) {
//                         error('Renseigne EKS_API_CIDR avec le CIDR public de Jenkins (par exemple x.x.x.x/32) avant Terraform.')
//                     }
//                 }
//                 withCredentials([
//                     string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
//                     string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
//                     string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
//                 ]) {
//                     withEnv(["AWS_REGION=${params.AWS_REGION}",
//                              "TF_VAR_aws_region=${params.AWS_REGION}",
//                              "TF_VAR_cluster_name=${params.EKS_CLUSTER_NAME}",
//                              "TF_VAR_cluster_endpoint_cidr=${params.EKS_API_CIDR}"]) {
//                         dir('infrastructure/terraform') {
//                             sh '''
//                                 set +x
//                                 terraform init -input=false
//                                 terraform fmt -check -recursive
//                                 terraform validate
//                                 terraform plan -input=false -out=tfplan
//                                 if [ "$APPLY_TERRAFORM" = "true" ]; then
//                                     terraform apply -input=false -auto-approve tfplan
//                                 fi
//                             '''
//                         }
//                     }
//                 }
//             }
//         }

//         stage('Déploiement sur EKS') {
//             when { expression { params.DEPLOY_TO_EKS } }
//             steps {
//                 withCredentials([
//                     string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
//                     string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
//                     string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
//                 ]) {
//                     withEnv(["AWS_REGION=${params.AWS_REGION}",
//                              "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
//                         dir('infrastructure/terraform') {
//                             script {
//                                 env.RDS_ENDPOINT = sh(returnStdout: true, script: 'terraform output -raw rds_endpoint').trim()
//                                 env.RDS_SECRET_ARN = sh(returnStdout: true, script: 'terraform output -raw rds_secret_arn').trim()
//                             }
//                         }
//                         sh '''
//                             set +x
//                             kubeconfig_file="$(mktemp)"
//                             secret_file="$(mktemp)"
//                             chmod 600 "$kubeconfig_file" "$secret_file"
//                             trap 'rm -f "$kubeconfig_file" "$secret_file"' EXIT
//                             export KUBECONFIG="$kubeconfig_file"

//                             aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"
//                             DB_SECRET_JSON="$(aws secretsmanager get-secret-value --secret-id "$RDS_SECRET_ARN" --query SecretString --output text)"
//                             printf '%s' "$DB_SECRET_JSON" | jq -er '"username=" + .username, "password=" + .password' > "$secret_file"

//                             kubectl apply -f k8s/namespace.yaml
//                             kubectl create secret generic stationski-db \
//                               --namespace stationski \
//                               --from-env-file="$secret_file" \
//                               --dry-run=client -o yaml | kubectl apply -f -
//                             kubectl create configmap stationski-backend-config \
//                               --namespace stationski \
//                               --from-literal="DB_URL=jdbc:mysql://$RDS_ENDPOINT:3306/skidb?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8" \
//                               --dry-run=client -o yaml | kubectl apply -f -

//                             # ---- ArgoCD (GitOps) : remplace les kubectl apply de backend.yaml / frontend.yaml ----
//                             # Installation d'ArgoCD
//                             kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
//                             kubectl apply -n argocd --server-side --force-conflicts \
//                               -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
//                             kubectl wait --for=condition=Established crd/applications.argoproj.io --timeout=120s

//                             # Interface web ArgoCD en HTTP, exposee par un NLB (noeuds en sous-reseau prive)
//                             kubectl -n argocd patch configmap argocd-cmd-params-cm --type merge \
//                               -p '{"data":{"server.insecure":"true"}}'
//                             kubectl -n argocd patch svc argocd-server \
//                               -p '{"metadata":{"annotations":{"service.beta.kubernetes.io/aws-load-balancer-type":"nlb","service.beta.kubernetes.io/aws-load-balancer-scheme":"internet-facing"}},"spec":{"type":"LoadBalancer","ports":[{"name":"http","port":80,"targetPort":8080,"nodePort":30081}]}}'
//                             kubectl -n argocd rollout restart deployment argocd-server
//                             kubectl -n argocd rollout status deployment/argocd-server --timeout=300s
//                             kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=300s
//                             kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s

//                             # Application geree par ArgoCD : dossier k8s/app du depot Git
//                             ARGO_REPO_URL="${GIT_REPO_URL:-$GIT_URL}"
//                             test -n "$ARGO_REPO_URL" || { echo "GIT_REPO_URL vide et GIT_URL indisponible" >&2; exit 1; }
//                             BACKEND_DEPLOY_IMAGE="$BACKEND_IMAGE:$IMAGE_TAG"
//                             FRONTEND_DEPLOY_IMAGE="$FRONTEND_IMAGE:$IMAGE_TAG"
//                             sed -e "s|REPO_URL_PLACEHOLDER|$ARGO_REPO_URL|g" \
//                                 -e "s|REVISION_PLACEHOLDER|$GIT_REVISION|g" \
//                                 -e "s|BACKEND_IMAGE_PLACEHOLDER|$BACKEND_DEPLOY_IMAGE|g" \
//                                 -e "s|FRONTEND_IMAGE_PLACEHOLDER|$FRONTEND_DEPLOY_IMAGE|g" \
//                                 k8s/argocd/application.yaml | kubectl apply -f -
//                             kubectl -n argocd annotate application station-ski argocd.argoproj.io/refresh=hard --overwrite

//                             # Attend qu'ArgoCD ait deploye les bonnes images
//                             CUR_BACK=""
//                             CUR_FRONT=""
//                             for i in $(seq 1 60); do
//                                 CUR_BACK=$(kubectl -n stationski get deploy stationski-backend \
//                                            -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
//                                 CUR_FRONT=$(kubectl -n stationski get deploy stationski-frontend \
//                                             -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
//                                 [ "$CUR_BACK" = "$BACKEND_DEPLOY_IMAGE" ] && [ "$CUR_FRONT" = "$FRONTEND_DEPLOY_IMAGE" ] && break
//                                 sleep 5
//                             done
//                             if [ "$CUR_BACK" != "$BACKEND_DEPLOY_IMAGE" ] || [ "$CUR_FRONT" != "$FRONTEND_DEPLOY_IMAGE" ]; then
//                                 echo "ArgoCD n'a pas synchronise les images attendues" >&2
//                                 kubectl -n argocd get applications
//                                 kubectl -n argocd describe application station-ski | tail -40
//                                 exit 1
//                             fi

//                             kubectl -n argocd get applications
//                             ARGO_HOST=""
//                             for i in $(seq 1 60); do
//                                 ARGO_HOST="$(kubectl -n argocd get svc argocd-server -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
//                                 [ -n "$ARGO_HOST" ] && break
//                                 sleep 5
//                             done
//                             echo "ArgoCD : http://$ARGO_HOST (utilisateur admin)"
//                             echo "Mot de passe : kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d"

//                             kubectl rollout status deployment/stationski-backend --namespace stationski --timeout=600s
//                             kubectl rollout status deployment/stationski-frontend --namespace stationski --timeout=300s
//                             frontend_host=""
//                             for attempt in $(seq 1 60); do
//                                 frontend_host="$(kubectl get service stationski-frontend --namespace stationski --output=jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
//                                 if [ -n "$frontend_host" ]; then break; fi
//                                 sleep 5
//                             done
//                             test -n "$frontend_host"
//                             printf 'Frontend URL: http://%s\\n' "$frontend_host"
//                             command -v curl >/dev/null 2>&1 || { echo 'curl est requis pour le smoke test EKS.' >&2; exit 1; }
//                             api_url="http://$frontend_host/stationSki/registration/skier/1"
//                             api_status=""
//                             for attempt in $(seq 1 12); do
//                                 api_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
//                                     --connect-timeout 5 --max-time 15 "$api_url" || true)"
//                                 case "$api_status" in
//                                     2??|404) break ;;
//                                 esac
//                                 sleep 5
//                             done
//                             case "$api_status" in
//                                 2??|404) printf 'Smoke test API réussi : HTTP %s\n' "$api_status" ;;
//                                 *) echo "Smoke test API échoué : HTTP ${api_status:-aucune réponse} sur $api_url" >&2; exit 1 ;;
//                             esac
//                             kubectl get deployments,services,pods --namespace stationski
//                         '''
//                     }
//                 }
//             }
//         }

//         stage('Monitoring Prometheus et Grafana') {
//             when { expression { params.DEPLOY_TO_EKS && params.DEPLOY_MONITORING } }
//             steps {
//                 withCredentials([
//                     string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
//                     string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
//                     string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN'),
//                     string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_PASS')
//                 ]) {
//                     withEnv(["AWS_REGION=${params.AWS_REGION}",
//                              "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
//                         sh '''
//                             set +x
//                             command -v helm >/dev/null 2>&1 || { echo 'helm est requis sur l agent Jenkins.' >&2; exit 1; }
//                             kubeconfig_file="$(mktemp)"
//                             chmod 600 "$kubeconfig_file"
//                             trap 'rm -f "$kubeconfig_file"' EXIT
//                             export KUBECONFIG="$kubeconfig_file"
//                             aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"

//                             helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
//                             helm repo update
//                             helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
//                               --namespace monitoring --create-namespace \
//                               -f k8s/monitoring/values.yaml \
//                               --set grafana.adminPassword="$GRAFANA_PASS" \
//                               --wait --timeout 15m

//                             kubectl -n monitoring get pods
//                             GRAFANA_HOST=""
//                             for i in $(seq 1 60); do
//                                 GRAFANA_HOST="$(kubectl -n monitoring get svc monitoring-grafana -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
//                                 [ -n "$GRAFANA_HOST" ] && break
//                                 sleep 5
//                             done
//                             echo "Grafana : http://$GRAFANA_HOST (utilisateur admin)"
//                         '''
//                     }
//                 }
//             }
//         }

//         stage('Pause : tester avant la destruction') {
//             when { expression { params.DEPLOY_TO_EKS && params.DESTROY_TERRAFORM } }
//             steps {
//                 withCredentials([
//                     string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
//                     string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
//                     string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
//                 ]) {
//                     withEnv(["AWS_REGION=${params.AWS_REGION}",
//                              "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
//                         sh '''
//                             set +x
//                             kubeconfig_file="$(mktemp)"
//                             chmod 600 "$kubeconfig_file"
//                             trap 'rm -f "$kubeconfig_file"' EXIT
//                             export KUBECONFIG="$kubeconfig_file"
//                             aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"

//                             host() { kubectl -n "$1" get svc "$2" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true; }
//                             echo "=========================================================="
//                             echo "Application : http://$(host stationski stationski-frontend)"
//                             echo "ArgoCD      : http://$(host argocd argocd-server)  (admin)"
//                             echo "Mot de passe ArgoCD : $(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' 2>/dev/null | base64 -d)"
//                             echo "Grafana     : http://$(host monitoring monitoring-grafana)  (admin)"
//                             echo "Prometheus  : http://$(host monitoring monitoring-kube-prometheus-prometheus):9090"
//                             echo "=========================================================="
//                             kubectl get nodes
//                             kubectl -n stationski get pods,svc
//                         '''
//                     }
//                 }
//                 script {
//                     try {
//                         timeout(time: params.TEST_MINUTES.toInteger(), unit: 'MINUTES') {
//                             input message: 'Infrastructure en ligne : teste maintenant. Clique pour detruire (sinon destruction automatique a la fin du delai).',
//                                   ok: 'Detruire maintenant'
//                         }
//                     } catch (err) {
//                         echo "Delai ecoule ou pause interrompue : on passe a la destruction."
//                     }
//                 }
//             }
//         }


//         stage('Terraform : destroy') {
//             when { expression { params.DESTROY_TERRAFORM } }
//             steps {
//                 script {
//                     if (!params.EKS_API_CIDR?.trim()) {
//                         error('Renseigne EKS_API_CIDR (même valeur que pour le apply).')
//                     }
//                 }
//                 withCredentials([
//                     string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
//                     string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
//                     string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
//                 ]) {
//                     withEnv(["AWS_REGION=${params.AWS_REGION}",
//                              "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}",
//                              "TF_VAR_aws_region=${params.AWS_REGION}",
//                              "TF_VAR_cluster_name=${params.EKS_CLUSTER_NAME}",
//                              "TF_VAR_cluster_endpoint_cidr=${params.EKS_API_CIDR}"]) {
//                         // Supprime d'abord le Service LoadBalancer (NLB) créé par Kubernetes,
//                         // sinon AWS refuse de supprimer le VPC (DependencyViolation).
//                         sh '''
//                             set +e
//                             set +x
//                             kubeconfig_file="$(mktemp)"
//                             trap 'rm -f "$kubeconfig_file"' EXIT
//                             export KUBECONFIG="$kubeconfig_file"
//                             aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"
//                             # ArgoCD d'abord (sinon selfHeal recree l'application), puis monitoring et namespaces
//                             # (supprime aussi les NLB d'ArgoCD et Grafana, sinon DependencyViolation sur le VPC)
//                             kubectl -n argocd delete application station-ski --ignore-not-found --timeout=120s
//                             helm uninstall monitoring -n monitoring
//                             kubectl delete namespace monitoring --ignore-not-found --timeout=300s
//                             kubectl delete namespace argocd --ignore-not-found --timeout=300s
//                             kubectl delete namespace stationski --timeout=300s
//                             exit 0
//                         '''
//                         dir('infrastructure/terraform') {
//                             sh '''
//                                 set +x
//                                 terraform init -input=false
//                                 terraform destroy -input=false -auto-approve
//                             '''
//                         }
//                     }
//                 }
//             }
//         }
//     }

//     post {
//         always {
//             junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
//             archiveArtifacts artifacts: 'backend/target/site/jacoco/**,backend/target/surefire-reports/*.xml',
//                              allowEmptyArchive: true
//             dir('backend') {
//                 sh './mvnw -B clean'
//             }
//             sh 'docker logout >/dev/null 2>&1 || true'
//         }
//         success {
//             echo "Build ${env.BUILD_NUMBER} OK — ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} et ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
//         }
//         failure {
//             echo "Build ${env.BUILD_NUMBER} en échec : consulte la Console Output et les rapports archivés."
//         }
//     }
// }


// Destruction de l'infrastructure AWS (Kubernetes puis Terraform).
// Appelée par le stage "Terraform : destroy" ET par post { cleanup } en cas d'échec ou d'annulation.
def destroyInfra() {
    withCredentials([
        string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
        string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
        string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
    ]) {
        withEnv(["AWS_REGION=${params.AWS_REGION}",
                 "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}",
                 "TF_VAR_aws_region=${params.AWS_REGION}",
                 "TF_VAR_cluster_name=${params.EKS_CLUSTER_NAME}",
                 "TF_VAR_cluster_endpoint_cidr=${params.EKS_API_CIDR}"]) {
            // Supprime d'abord les Services LoadBalancer (NLB) créés par Kubernetes,
            // sinon AWS refuse de supprimer le VPC (DependencyViolation).
            // Ne bloque jamais si le cluster est injoignable.
            sh '''
                set +e
                set +x
                kubeconfig_file="$(mktemp)"
                trap 'rm -f "$kubeconfig_file"' EXIT
                export KUBECONFIG="$kubeconfig_file"
                if aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"; then
                    # ArgoCD d'abord (sinon selfHeal recree l'application), puis monitoring et namespaces
                    kubectl --request-timeout=30s -n argocd delete application station-ski --ignore-not-found --timeout=120s
                    helm uninstall monitoring -n monitoring
                    kubectl --request-timeout=30s delete namespace monitoring --ignore-not-found --timeout=300s
                    kubectl --request-timeout=30s delete namespace argocd --ignore-not-found --timeout=300s
                    kubectl --request-timeout=30s delete namespace stationski --ignore-not-found --timeout=300s
                fi
                exit 0
            '''
            dir('infrastructure/terraform') {
                sh '''
                    set +x
                    terraform init -input=false
                    terraform destroy -input=false -auto-approve
                '''
            }
        }
    }
}

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 120, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyser le backend sur SonarQube.')
        booleanParam(name: 'PUBLISH_NEXUS', defaultValue: true, description: 'Publier le JAR dans le dépôt Maven Nexus.')
        booleanParam(name: 'PUSH_IMAGES', defaultValue: true, description: 'Publier les images backend et frontend sur Docker Hub.')
        booleanParam(name: 'PLAN_TERRAFORM', defaultValue: true, description: 'Vérifier Terraform et afficher le plan sans appliquer ni déployer.')
        booleanParam(name: 'APPLY_TERRAFORM', defaultValue: true, description: 'Créer ou mettre à jour VPC, EKS et RDS (coûts AWS).')
        booleanParam(name: 'DEPLOY_TO_EKS', defaultValue: true, description: 'Déployer les images sur le cluster EKS.')
        booleanParam(name: 'DESTROY_TERRAFORM', defaultValue: true, description: 'DANGER : supprime VPC, EKS et RDS (données perdues) à la fin du build, ou automatiquement si le build échoue après le apply.')
        booleanParam(name: 'DEPLOY_MONITORING', defaultValue: true, description: 'Installer Prometheus et Grafana (Helm) apres le deploiement sur EKS.')
        string(name: 'GIT_REVISION', defaultValue: 'feature/hajer-iben-nasr', description: 'Branche Git suivie par ArgoCD (les fichiers k8s/app doivent etre pousses sur cette branche).')
        string(name: 'TEST_MINUTES', defaultValue: '15', description: 'Duree de la pause pour tester (Grafana, ArgoCD, application) avant la destruction automatique.')
        string(name: 'GIT_REPO_URL', defaultValue: 'https://github.com/fida-ghourabi/GL5-G2-StationSki.git', description: 'URL HTTPS du depot GitHub pour ArgoCD (laisser vide = URL du checkout Jenkins).')
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'Région du Learner Lab.')
        string(name: 'EKS_CLUSTER_NAME', defaultValue: 'stationski-cluster', description: 'Nom du cluster Terraform/EKS.')
        string(name: 'EKS_API_CIDR', defaultValue: '', description: 'CIDR public restreint pour Jenkins vers l’API EKS, par exemple son IP sortante suivie de /32.')
        string(name: 'DOCKERHUB_NAMESPACE', defaultValue: 'bennasserhajer', description: 'Namespace Docker Hub propriétaire des images.')
        string(name: 'SONAR_URL', defaultValue: 'http://sonarqube:9000', description: 'URL SonarQube joignable depuis Jenkins.')
        string(name: 'NEXUS_MAVEN_URL', defaultValue: 'http://nexus:8081/repository/maven-snapshots/', description: 'URL du dépôt Maven snapshots Nexus.')
    }

    environment {
        DOCKER_CREDENTIALS    = 'dockerhub'
        SONAR_CREDENTIALS     = 'sonar-token'
        NEXUS_CREDENTIALS     = 'nexus-creds'
        AWS_ACCESS_KEY_ID_CREDENTIALS     = 'aws-access-key-id'
        AWS_SECRET_ACCESS_KEY_CREDENTIALS = 'aws-secret-access-key'
        AWS_SESSION_TOKEN_CREDENTIALS     = 'aws-session-token'
        IMAGE_TAG = 'pending'
        BACKEND_IMAGE = "${params.DOCKERHUB_NAMESPACE}/stationski-backend"
        FRONTEND_IMAGE = "${params.DOCKERHUB_NAMESPACE}/stationski-frontend"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                }
            }
        }

        stage('Vérification des outils') {
            steps {
                sh '''
                    set -eu
                    java -version
                    chmod +x backend/mvnw
                    ./backend/mvnw -version
                    command -v docker
                    command -v terraform
                    command -v aws
                    command -v kubectl
                    command -v jq
                    command -v python3
                '''
            }
        }

        stage('Tests backend et couverture') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw && ./mvnw -B clean verify'
                }
            }
        }

        stage('Analyse SonarQube') {
            when { expression { params.RUN_SONAR } }
            steps {
                withCredentials([string(credentialsId: env.SONAR_CREDENTIALS, variable: 'SONAR_TOKEN')]) {
                    dir('backend') {
                        sh '''
                            ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
                              -Dsonar.projectKey=stationski-inscription \
                              -Dsonar.host.url="$SONAR_URL" \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Publication du JAR vers Nexus') {
            when { expression { params.PUBLISH_NEXUS } }
            steps {
                withCredentials([usernamePassword(credentialsId: env.NEXUS_CREDENTIALS,
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASSWORD')]) {
                    dir('backend') {
                        sh '''
                            set +x
                            chmod +x mvnw
                            settings_file="$(mktemp)"
                            trap 'rm -f "$settings_file"' EXIT
                            python3 -c 'import os, xml.sax.saxutils; print("<settings><servers><server><id>stationski-nexus</id><username>" + xml.sax.saxutils.escape(os.environ["NEXUS_USER"]) + "</username><password>" + xml.sax.saxutils.escape(os.environ["NEXUS_PASSWORD"]) + "</password></server></servers></settings>")' > "$settings_file"
                            ./mvnw -B -s "$settings_file" org.apache.maven.plugins:maven-deploy-plugin:3.1.3:deploy-file \
                              -DrepositoryId=stationski-nexus \
                              -Durl="$NEXUS_MAVEN_URL" \
                              -Dfile=target/gestion-station-skii-0.0.1-SNAPSHOT.jar \
                              -DpomFile=pom.xml \
                              -DgeneratePom=false
                        '''
                    }
                }
            }
        }

        stage('Construction des images') {
            steps {
                sh '''
                    docker build -t "$BACKEND_IMAGE:$IMAGE_TAG" backend
                    docker build -t "$FRONTEND_IMAGE:$IMAGE_TAG" frontend
                '''
            }
        }

        stage('Publication Docker Hub') {
            when { expression { params.PUSH_IMAGES || params.DEPLOY_TO_EKS } }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS,
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        set +x
                        printf '%s' "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USER" --password-stdin
                        docker push "$BACKEND_IMAGE:$IMAGE_TAG"
                        docker push "$FRONTEND_IMAGE:$IMAGE_TAG"
                    '''
                }
            }
        }

        stage('Vérification connexion AWS') {
            when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS || params.DESTROY_TERRAFORM } }
            steps {
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}", "AWS_DEFAULT_REGION=${params.AWS_REGION}"]) {
                        sh '''
                            set +x
                            echo 'Vérification de la connexion AWS et de l’identité utilisée par Jenkins...'
                            aws sts get-caller-identity --output table
                        '''
                    }
                }
            }
        }

        stage('Terraform : plan et provisionnement') {
            when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS } }
            steps {
                script {
                    if (!params.EKS_API_CIDR?.trim()) {
                        error('Renseigne EKS_API_CIDR avec le CIDR public de Jenkins (par exemple x.x.x.x/32) avant Terraform.')
                    }
                    // Marque l'infra comme "touchée" AVANT le apply : si le build échoue ensuite,
                    // post { cleanup } saura qu'il faut détruire.
                    if (params.APPLY_TERRAFORM) {
                        env.INFRA_TOUCHED = 'true'
                    }
                }
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "TF_VAR_aws_region=${params.AWS_REGION}",
                             "TF_VAR_cluster_name=${params.EKS_CLUSTER_NAME}",
                             "TF_VAR_cluster_endpoint_cidr=${params.EKS_API_CIDR}"]) {
                        dir('infrastructure/terraform') {
                            sh '''
                                set +x
                                terraform init -input=false
                                terraform fmt -check -recursive
                                terraform validate
                                terraform plan -input=false -out=tfplan
                                if [ "$APPLY_TERRAFORM" = "true" ]; then
                                    terraform apply -input=false -auto-approve tfplan
                                fi
                            '''
                        }
                    }
                }
            }
        }

        stage('Déploiement sur EKS') {
            when { expression { params.DEPLOY_TO_EKS } }
            steps {
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
                        dir('infrastructure/terraform') {
                            script {
                                env.RDS_ENDPOINT = sh(returnStdout: true, script: 'terraform output -raw rds_endpoint').trim()
                                env.RDS_SECRET_ARN = sh(returnStdout: true, script: 'terraform output -raw rds_secret_arn').trim()
                            }
                        }
                        sh '''
                            set +x
                            kubeconfig_file="$(mktemp)"
                            secret_file="$(mktemp)"
                            chmod 600 "$kubeconfig_file" "$secret_file"
                            trap 'rm -f "$kubeconfig_file" "$secret_file"' EXIT
                            export KUBECONFIG="$kubeconfig_file"

                            aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"
                            DB_SECRET_JSON="$(aws secretsmanager get-secret-value --secret-id "$RDS_SECRET_ARN" --query SecretString --output text)"
                            printf '%s' "$DB_SECRET_JSON" | jq -er '"username=" + .username, "password=" + .password' > "$secret_file"

                            kubectl apply -f k8s/namespace.yaml
                            kubectl create secret generic stationski-db \
                              --namespace stationski \
                              --from-env-file="$secret_file" \
                              --dry-run=client -o yaml | kubectl apply -f -
                            kubectl create configmap stationski-backend-config \
                              --namespace stationski \
                              --from-literal="DB_URL=jdbc:mysql://$RDS_ENDPOINT:3306/skidb?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8" \
                              --dry-run=client -o yaml | kubectl apply -f -

                            # ---- ArgoCD (GitOps) : remplace les kubectl apply de backend.yaml / frontend.yaml ----
                            # Installation d'ArgoCD
                            kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
                            kubectl apply -n argocd --server-side --force-conflicts \
                              -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
                            kubectl wait --for=condition=Established crd/applications.argoproj.io --timeout=120s

                            # Interface web ArgoCD en HTTP, exposee par un NLB (noeuds en sous-reseau prive)
                            kubectl -n argocd patch configmap argocd-cmd-params-cm --type merge \
                              -p '{"data":{"server.insecure":"true"}}'
                            kubectl -n argocd patch svc argocd-server \
                              -p '{"metadata":{"annotations":{"service.beta.kubernetes.io/aws-load-balancer-type":"nlb","service.beta.kubernetes.io/aws-load-balancer-scheme":"internet-facing"}},"spec":{"type":"LoadBalancer","ports":[{"name":"http","port":80,"targetPort":8080,"nodePort":30081}]}}'
                            kubectl -n argocd rollout restart deployment argocd-server
                            kubectl -n argocd rollout status deployment/argocd-server --timeout=300s
                            kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=300s
                            kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s

                            # Application geree par ArgoCD : dossier k8s/app du depot Git
                            ARGO_REPO_URL="${GIT_REPO_URL:-$GIT_URL}"
                            test -n "$ARGO_REPO_URL" || { echo "GIT_REPO_URL vide et GIT_URL indisponible" >&2; exit 1; }
                            BACKEND_DEPLOY_IMAGE="$BACKEND_IMAGE:$IMAGE_TAG"
                            FRONTEND_DEPLOY_IMAGE="$FRONTEND_IMAGE:$IMAGE_TAG"
                            sed -e "s|REPO_URL_PLACEHOLDER|$ARGO_REPO_URL|g" \
                                -e "s|REVISION_PLACEHOLDER|$GIT_REVISION|g" \
                                -e "s|BACKEND_IMAGE_PLACEHOLDER|$BACKEND_DEPLOY_IMAGE|g" \
                                -e "s|FRONTEND_IMAGE_PLACEHOLDER|$FRONTEND_DEPLOY_IMAGE|g" \
                                k8s/argocd/application.yaml | kubectl apply -f -
                            kubectl -n argocd annotate application station-ski argocd.argoproj.io/refresh=hard --overwrite

                            # Attend qu'ArgoCD ait deploye les bonnes images
                            CUR_BACK=""
                            CUR_FRONT=""
                            for i in $(seq 1 60); do
                                CUR_BACK=$(kubectl -n stationski get deploy stationski-backend \
                                           -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                                CUR_FRONT=$(kubectl -n stationski get deploy stationski-frontend \
                                            -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                                [ "$CUR_BACK" = "$BACKEND_DEPLOY_IMAGE" ] && [ "$CUR_FRONT" = "$FRONTEND_DEPLOY_IMAGE" ] && break
                                sleep 5
                            done
                            if [ "$CUR_BACK" != "$BACKEND_DEPLOY_IMAGE" ] || [ "$CUR_FRONT" != "$FRONTEND_DEPLOY_IMAGE" ]; then
                                echo "ArgoCD n'a pas synchronise les images attendues" >&2
                                kubectl -n argocd get applications
                                kubectl -n argocd describe application station-ski | tail -40
                                exit 1
                            fi

                            kubectl -n argocd get applications
                            ARGO_HOST=""
                            for i in $(seq 1 60); do
                                ARGO_HOST="$(kubectl -n argocd get svc argocd-server -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
                                [ -n "$ARGO_HOST" ] && break
                                sleep 5
                            done
                            echo "ArgoCD : http://$ARGO_HOST (utilisateur admin)"
                            echo "Mot de passe : kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d"

                            kubectl rollout status deployment/stationski-backend --namespace stationski --timeout=600s
                            kubectl rollout status deployment/stationski-frontend --namespace stationski --timeout=300s
                            frontend_host=""
                            for attempt in $(seq 1 60); do
                                frontend_host="$(kubectl get service stationski-frontend --namespace stationski --output=jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
                                if [ -n "$frontend_host" ]; then break; fi
                                sleep 5
                            done
                            test -n "$frontend_host"
                            printf 'Frontend URL: http://%s\\n' "$frontend_host"
                            command -v curl >/dev/null 2>&1 || { echo 'curl est requis pour le smoke test EKS.' >&2; exit 1; }
                            api_url="http://$frontend_host/stationSki/registration/skier/1"
                            api_status=""
                            for attempt in $(seq 1 12); do
                                api_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
                                    --connect-timeout 5 --max-time 15 "$api_url" || true)"
                                case "$api_status" in
                                    2??|404) break ;;
                                esac
                                sleep 5
                            done
                            case "$api_status" in
                                2??|404) printf 'Smoke test API réussi : HTTP %s\n' "$api_status" ;;
                                *) echo "Smoke test API échoué : HTTP ${api_status:-aucune réponse} sur $api_url" >&2; exit 1 ;;
                            esac
                            kubectl get deployments,services,pods --namespace stationski
                        '''
                    }
                }
            }
        }

        stage('Monitoring Prometheus et Grafana') {
            when { expression { params.DEPLOY_TO_EKS && params.DEPLOY_MONITORING } }
            steps {
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN'),
                    string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_PASS')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
                        sh '''
                            set +x
                            command -v helm >/dev/null 2>&1 || { echo 'helm est requis sur l agent Jenkins.' >&2; exit 1; }
                            kubeconfig_file="$(mktemp)"
                            chmod 600 "$kubeconfig_file"
                            trap 'rm -f "$kubeconfig_file"' EXIT
                            export KUBECONFIG="$kubeconfig_file"
                            aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"

                            helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
                            helm repo update
                            helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
                              --namespace monitoring --create-namespace \
                              -f k8s/monitoring/values.yaml \
                              --set grafana.adminPassword="$GRAFANA_PASS" \
                              --wait --timeout 15m

                            kubectl -n monitoring get pods
                            GRAFANA_HOST=""
                            for i in $(seq 1 60); do
                                GRAFANA_HOST="$(kubectl -n monitoring get svc monitoring-grafana -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
                                [ -n "$GRAFANA_HOST" ] && break
                                sleep 5
                            done
                            echo "Grafana : http://$GRAFANA_HOST (utilisateur admin)"
                        '''
                    }
                }
            }
        }

        stage('Pause : tester avant la destruction') {
            when { expression { params.DEPLOY_TO_EKS && params.DESTROY_TERRAFORM } }
            steps {
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
                        sh '''
                            set +x
                            kubeconfig_file="$(mktemp)"
                            chmod 600 "$kubeconfig_file"
                            trap 'rm -f "$kubeconfig_file"' EXIT
                            export KUBECONFIG="$kubeconfig_file"
                            aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"

                            host() { kubectl -n "$1" get svc "$2" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true; }
                            echo "=========================================================="
                            echo "Application : http://$(host stationski stationski-frontend)"
                            echo "ArgoCD      : http://$(host argocd argocd-server)  (admin)"
                            echo "Mot de passe ArgoCD : $(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' 2>/dev/null | base64 -d)"
                            echo "Grafana     : http://$(host monitoring monitoring-grafana)  (admin)"
                            echo "Prometheus  : http://$(host monitoring monitoring-kube-prometheus-prometheus):9090"
                            echo "=========================================================="
                            kubectl get nodes
                            kubectl -n stationski get pods,svc
                        '''
                    }
                }
                script {
                    try {
                        timeout(time: params.TEST_MINUTES.toInteger(), unit: 'MINUTES') {
                            input message: 'Infrastructure en ligne : teste maintenant. Clique pour detruire (sinon destruction automatique a la fin du delai).',
                                  ok: 'Detruire maintenant'
                        }
                    } catch (err) {
                        echo "Delai ecoule ou pause interrompue : on passe a la destruction."
                    }
                }
            }
        }

        stage('Terraform : destroy') {
            when { expression { params.DESTROY_TERRAFORM } }
            steps {
                script {
                    if (!params.EKS_API_CIDR?.trim()) {
                        error('Renseigne EKS_API_CIDR (même valeur que pour le apply).')
                    }
                    destroyInfra()
                    env.INFRA_DESTROYED = 'true'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'backend/target/site/jacoco/**,backend/target/surefire-reports/*.xml',
                             allowEmptyArchive: true
            dir('backend') {
                sh './mvnw -B clean'
            }
            sh 'docker logout >/dev/null 2>&1 || true'
        }
        success {
            echo "Build ${env.BUILD_NUMBER} OK — ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} et ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
        }
        failure {
            echo "Build ${env.BUILD_NUMBER} en échec : consulte la Console Output et les rapports archivés."
        }
        // S'exécute en dernier, quel que soit le résultat (succès, échec, annulation).
        // Détruit l'infra si elle a été créée et n'a pas déjà été détruite.
        cleanup {
            script {
                if (params.DESTROY_TERRAFORM && env.INFRA_TOUCHED == 'true' && env.INFRA_DESTROYED != 'true') {
                    echo 'Build interrompu : destruction automatique de l’infrastructure AWS.'
                    try {
                        destroyInfra()
                    } catch (err) {
                        echo "ATTENTION : la destruction a échoué, lance terraform destroy à la main. Détail : ${err}"
                        throw err
                    }
                }
            }
        }
    }
}
