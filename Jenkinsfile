// Fournit les identifiants AWS Academy (temporaires) aux etapes qui en ont besoin
def withAws(Closure body) {
    withCredentials([
        string(credentialsId: 'aws-access-key',    variable: 'AWS_ACCESS_KEY_ID'),
        string(credentialsId: 'aws-secret-key',    variable: 'AWS_SECRET_ACCESS_KEY'),
        string(credentialsId: 'aws-session-token', variable: 'AWS_SESSION_TOKEN')
    ]) {
        body()
    }
}

// true si le dossier a change depuis le dernier commit EFFECTIVEMENT construit (image poussee),
// ou si aucun etat n'est memorise. Independant du resultat global des builds Jenkins.
def changedSince(String dir, String head) {
    def last = sh(script: "cat \"\$BUILD_STATE_DIR/${dir}.commit\" 2>/dev/null || true",
                  returnStdout: true).trim()
    if (!last) {
        return true
    }
    // code retour 0 = aucun changement ; autre = changement (ou commit memorise introuvable)
    return sh(script: "git diff --quiet ${last} ${head} -- ${dir}", returnStatus: true) != 0
}

pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    parameters {
        booleanParam(name: 'FORCE_BUILD', defaultValue: false,
                     description: 'Forcer build, tests, Sonar et images meme sans changement de code (a cocher pour la validation)')
        choice(name: 'ACTION',
               choices: ['apply', 'plan', 'destroy'],
               description: 'apply = build + EKS + deploiement | plan = jusqu au terraform plan | destroy = supprime EKS')
    }

    environment {
        ACTION             = "${params.ACTION ?: 'apply'}"

        registry           = 'saharsaidani/gestion-station-skii'
        registryCredential = 'dockerhub'
        FRONTEND_REGISTRY  = 'saharsaidani/gestion-station-skii-frontend'

        SONAR_URL          = 'http://sonarqube:9000'
        NEXUS_SNAPSHOTS    = 'http://nexus:8081/repository/maven-snapshots/'
        BACKEND_DIR        = 'backend'
        TERRAFORM_DIR      = 'terraform'
        TF_IN_AUTOMATION   = 'true'
        TF_PLUGIN_CACHE_DIR = '/var/jenkins_home/.terraform.d/plugin-cache'
        // Etat Terraform conserve hors du workspace
        TF_STATE           = '/var/jenkins_home/tfstate/stationski.tfstate'

        AWS_DEFAULT_REGION = 'us-east-1'
        CLUSTER_NAME       = 'stationski'
        NODEGROUP_NAME     = 'stationski-nodes'
        K8S_NAMESPACE      = 'station-ski'
        GIT_REVISION       = 'feature/sahar-saidani'
        // Dernier commit construit (backend/frontend), conserve hors du workspace
        BUILD_STATE_DIR    = '/var/jenkins_home/build-state'

        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
        TEST_DB_URL        = 'jdbc:mysql://mysql:3306/station_test?createDatabaseIfNotExist=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 120, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('1 - Récupération du code (Git)') {
            steps {
                checkout scm
            }
        }

        stage('2 - Détection des changements de code') {
            when { expression { env.ACTION != 'destroy' } }
            steps {
                script {
                    sh 'mkdir -p "$BUILD_STATE_DIR"'
                    env.HEAD_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    def backend = true
                    def frontend = true
                    if (!params.FORCE_BUILD) {
                        backend  = changedSince('backend',  env.HEAD_COMMIT)
                        frontend = changedSince('frontend', env.HEAD_COMMIT)
                    }
                    env.BACKEND_CHANGED  = backend.toString()
                    env.FRONTEND_CHANGED = frontend.toString()
                    echo "Commit : ${env.HEAD_COMMIT} | Backend modifié : ${env.BACKEND_CHANGED} | Frontend modifié : ${env.FRONTEND_CHANGED} | FORCE_BUILD : ${params.FORCE_BUILD}"
                }
            }
        }

        stage('3 - Nettoyage Maven') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B clean'
                }
            }
        }

        stage('4 - Compilation et package du JAR') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B package -DskipTests'
                }
            }
        }

        stage('5 - Tests unitaires (JUnit + Mockito)') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'mysql-test',
                        usernameVariable: 'TEST_DB_USERNAME',
                        passwordVariable: 'TEST_DB_PASSWORD')]) {
                    dir("${BACKEND_DIR}") {
                        sh 'mvn -B test'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('6 - Analyse qualité SonarQube') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    dir("${BACKEND_DIR}") {
                        sh '''
                            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                              -Dsonar.projectKey=gestion-station-skii \
                              -Dsonar.host.url=$SONAR_URL \
                              -Dsonar.token="$SONAR_TOKEN" \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('7 - Publication du JAR sur Nexus') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'nexus-creds',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS')]) {
                    sh '''
                        cat > "$WORKSPACE/settings-nexus.xml" <<EOF
<settings>
    <servers>
        <server>
            <id>nexus-snapshots</id>
            <username>$NEXUS_USER</username>
            <password>$NEXUS_PASS</password>
        </server>
    </servers>
</settings>
EOF
                    '''
                    dir("${BACKEND_DIR}") {
                        sh '''
                            mvn -B deploy -DskipTests \
                              -s "$WORKSPACE/settings-nexus.xml" \
                              -DaltDeploymentRepository=nexus-snapshots::default::$NEXUS_SNAPSHOTS
                        '''
                    }
                    sh 'rm -f "$WORKSPACE/settings-nexus.xml"'
                }
            }
        }

        stage('8 - Build image Docker backend') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                script {
                    def pomVersion = sh(
                        script: "cd ${BACKEND_DIR} && mvn -q -B help:evaluate -Dexpression=project.version -DforceStdout",
                        returnStdout: true
                    ).trim()
                    env.IMAGE_TAG = "${pomVersion.replace('-SNAPSHOT', '')}-${BUILD_NUMBER}"
                    echo "Image : ${registry}:${env.IMAGE_TAG}"
                    docker.build("${registry}:${env.IMAGE_TAG}", "${BACKEND_DIR}")
                }
            }
        }

        stage('9 - Push image backend sur Docker Hub') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                script {
                    def backendImage = docker.image("${registry}:${env.IMAGE_TAG}")
                    docker.withRegistry('', registryCredential) {
                        retry(3) { backendImage.push() }
                        retry(3) { backendImage.push('latest') }
                    }
                    // Memorise le commit seulement apres un push reussi
                    sh 'echo "$HEAD_COMMIT" > "$BUILD_STATE_DIR/backend.commit"'
                }
            }
        }

        stage('10 - Image Docker frontend (build + push)') {
            when { expression { env.ACTION != 'destroy' && env.FRONTEND_CHANGED == 'true' } }
            steps {
                script {
                    def frontImage = "${FRONTEND_REGISTRY}:front-${BUILD_NUMBER}"
                    echo "Image : ${frontImage}"
                    retry(3) { sh "docker build -t ${frontImage} frontend" }
                    docker.withRegistry('', registryCredential) {
                        retry(3) { sh "docker push ${frontImage}" }
                        sh "docker tag ${frontImage} ${FRONTEND_REGISTRY}:latest"
                        retry(3) { sh "docker push ${FRONTEND_REGISTRY}:latest" }
                    }
                    sh 'echo "$HEAD_COMMIT" > "$BUILD_STATE_DIR/frontend.commit"'
                }
            }
        }

        stage('11 - Terraform : validation') {
            when { expression { env.ACTION != 'destroy' } }
            steps {
                dir("${TERRAFORM_DIR}") {
                    sh 'terraform version'
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        sh 'terraform fmt -check -recursive -diff'
                    }
                    sh 'mkdir -p "$TF_PLUGIN_CACHE_DIR"'
                    retry(3) { sh 'terraform init -input=false' }
                    sh 'terraform validate'
                }
            }
        }

        stage('12 - Terraform : plan') {
            when { expression { env.ACTION == 'apply' || env.ACTION == 'plan' } }
            steps {
                script {
                    withAws {
                        dir("${TERRAFORM_DIR}") {
                            sh 'mkdir -p "$TF_PLUGIN_CACHE_DIR" "$(dirname "$TF_STATE")"'
                            retry(3) { sh 'terraform init -input=false' }
                            sh 'terraform plan -input=false -state="$TF_STATE"'
                        }
                    }
                }
            }
        }

        stage('13 - Validation manuelle') {
            when { expression { env.ACTION == 'apply' || env.ACTION == 'destroy' } }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "Confirmer terraform ${env.ACTION} sur EKS (ressources facturees) ?",
                          ok: 'Confirmer'
                }
            }
        }

        stage('14 - Terraform : création du cluster EKS') {
            when { expression { env.ACTION == 'apply' } }
            steps {
                script {
                    withAws {
                        dir("${TERRAFORM_DIR}") {
                            sh 'terraform apply -input=false -auto-approve -state="$TF_STATE"'
                        }
                    }
                }
            }
        }

        stage('15 - Déploiement MySQL sur EKS') {
            when { expression { env.ACTION == 'apply' } }
            steps {
                script {
                    env.DEPLOY_IMAGE = "${registry}:${env.IMAGE_TAG ?: 'latest'}"
                    withAws {
                        withCredentials([
                            usernamePassword(credentialsId: 'mysql-app',
                                             usernameVariable: 'DB_USER',
                                             passwordVariable: 'DB_PASS'),
                            string(credentialsId: 'mysql-root-password', variable: 'DB_ROOT_PASS')
                        ]) {
                            sh '''
                                export KUBECONFIG="$WORKSPACE/kubeconfig-eks"
                                aws eks update-kubeconfig --region "$AWS_DEFAULT_REGION" \
                                  --name "$CLUSTER_NAME" --kubeconfig "$KUBECONFIG"

                                # 1. Attendre que le node group soit ACTIVE cote AWS
                                aws eks wait nodegroup-active --cluster-name "$CLUSTER_NAME" \
                                  --nodegroup-name "$NODEGROUP_NAME" --region "$AWS_DEFAULT_REGION"

                                # 2. Attendre que les noeuds apparaissent dans Kubernetes
                                N=0
                                for i in $(seq 1 60); do
                                    N=$(kubectl get nodes --no-headers 2>/dev/null | wc -l)
                                    [ "$N" -gt 0 ] && break
                                    echo "En attente de noeuds ($i/60)..."
                                    sleep 10
                                done
                                if [ "$N" -eq 0 ]; then
                                    echo "=== AUCUN NOEUD ==="
                                    aws eks describe-nodegroup --cluster-name "$CLUSTER_NAME" \
                                      --nodegroup-name "$NODEGROUP_NAME" --region "$AWS_DEFAULT_REGION" \
                                      --query 'nodegroup.{status:status,scaling:scalingConfig,health:health}'
                                    exit 1
                                fi

                                # 3. Attendre qu'ils soient Ready
                                kubectl wait --for=condition=Ready nodes --all --timeout=600s
                                kubectl get nodes -o wide

                                kubectl create namespace "$K8S_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
                                kubectl -n "$K8S_NAMESPACE" create secret generic mysql-credentials \
                                  --from-literal=MYSQL_USER="$DB_USER" \
                                  --from-literal=MYSQL_PASSWORD="$DB_PASS" \
                                  --from-literal=MYSQL_ROOT_PASSWORD="$DB_ROOT_PASS" \
                                  --from-literal=MYSQL_DATABASE=skidb \
                                  --dry-run=client -o yaml | kubectl apply -f -

                                kubectl apply -f k8s/storageclass.yaml
                                kubectl apply -f k8s/mysql.yaml
                                kubectl -n "$K8S_NAMESPACE" rollout status deployment/mysql --timeout=300s || {
                                    echo "=== DIAGNOSTIC MYSQL ==="
                                    kubectl -n "$K8S_NAMESPACE" get pods,pvc -o wide
                                    kubectl -n "$K8S_NAMESPACE" describe pvc mysql-data | tail -25
                                    kubectl -n "$K8S_NAMESPACE" describe pod -l app=mysql | tail -35
                                    kubectl -n "$K8S_NAMESPACE" logs -l app=mysql --tail=40 || true
                                    kubectl -n kube-system get pods | grep -i ebs || true
                                    kubectl -n kube-system logs -l app=ebs-csi-controller -c csi-provisioner --tail=30 || true
                                    exit 1
                                }

                                kubectl -n "$K8S_NAMESPACE" get pods,pvc
                            '''
                        }
                    }
                }
            }
        }

        stage('16 - ArgoCD : déploiement GitOps de l application') {
            when { expression { env.ACTION == 'apply' } }
            steps {
                script {
                    env.DEPLOY_IMAGE = "${registry}:${env.IMAGE_TAG ?: 'latest'}"
                    withAws {
                        sh '''
                            export KUBECONFIG="$WORKSPACE/kubeconfig-eks"
                            aws eks update-kubeconfig --region "$AWS_DEFAULT_REGION" \
                              --name "$CLUSTER_NAME" --kubeconfig "$KUBECONFIG"

                            # Installation d'ArgoCD
                            kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
                            kubectl apply -n argocd --server-side --force-conflicts \
                              -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
                            kubectl wait --for=condition=Established crd/applications.argoproj.io --timeout=120s

                            # Interface web en HTTP sur le NodePort 30081
                            kubectl -n argocd patch configmap argocd-cmd-params-cm --type merge \
                              -p '{"data":{"server.insecure":"true"}}'
                            kubectl -n argocd patch svc argocd-server \
                              -p '{"spec":{"type":"NodePort","ports":[{"name":"http","port":80,"targetPort":8080,"nodePort":30081}]}}'
                            kubectl -n argocd rollout restart deployment argocd-server
                            kubectl -n argocd rollout status deployment/argocd-server --timeout=300s
                            kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=300s
                            kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s

                            # Application geree par ArgoCD (GitOps) : chemin k8s/app du depot
                            sed -e "s|IMAGE_PLACEHOLDER|$DEPLOY_IMAGE|g" \
                                -e "s|REVISION_PLACEHOLDER|$GIT_REVISION|g" \
                                k8s/argocd/application.yaml | kubectl apply -f -
                            kubectl -n argocd annotate application station-ski argocd.argoproj.io/refresh=hard --overwrite

                            # Attend qu'ArgoCD ait deploye la bonne image
                            CUR=""
                            for i in $(seq 1 120); do
                                CUR=$(kubectl -n "$K8S_NAMESPACE" get deploy station-ski \
                                      -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                                [ "$CUR" = "$DEPLOY_IMAGE" ] && break
                                sleep 5
                            done
                            [ "$CUR" = "$DEPLOY_IMAGE" ] || {
                                echo "ArgoCD n'a pas synchronise l'image attendue ($DEPLOY_IMAGE)"
                                kubectl -n argocd get applications
                                kubectl -n argocd describe application station-ski | tail -40
                                exit 1
                            }
                            kubectl -n "$K8S_NAMESPACE" rollout status deployment/station-ski --timeout=600s
                            kubectl -n argocd get applications
                            kubectl -n "$K8S_NAMESPACE" get pods,svc

                            NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
                            echo "=========================================================="
                            echo "Application : http://$NODE_IP:30080/stationSki/swagger-ui/index.html"
                            echo "ArgoCD      : http://$NODE_IP:30081  (utilisateur admin)"
                            echo "Mot de passe ArgoCD : kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d"
                            echo "=========================================================="
                        '''
                    }
                }
            }
        }

        stage('17 - Monitoring Prometheus et Grafana') {
            when { expression { env.ACTION == 'apply' } }
            steps {
                script {
                    withAws {
                        withCredentials([string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_PASS')]) {
                            sh '''
                                export KUBECONFIG="$WORKSPACE/kubeconfig-eks"
                                aws eks update-kubeconfig --region "$AWS_DEFAULT_REGION" \
                                  --name "$CLUSTER_NAME" --kubeconfig "$KUBECONFIG"

                                helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
                                helm repo update
                                helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
                                  --namespace monitoring --create-namespace \
                                  -f k8s/monitoring/values.yaml \
                                  --set grafana.adminPassword="$GRAFANA_PASS" \
                                  --wait --timeout 15m

                                kubectl -n monitoring get pods
                                NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
                                echo "=========================================================="
                                echo "Grafana : http://$NODE_IP:30300  (utilisateur admin)"
                                echo "=========================================================="
                            '''
                        }
                    }
                }
            }
        }

        stage('18 - Terraform : destruction du cluster EKS') {
            when { expression { env.ACTION == 'destroy' } }
            steps {
                script {
                    withAws {
                        // Supprime d abord les volumes EBS crees par Kubernetes
                        sh '''
                            export KUBECONFIG="$WORKSPACE/kubeconfig-eks"
                            aws eks update-kubeconfig --region "$AWS_DEFAULT_REGION" \
                              --name "$CLUSTER_NAME" --kubeconfig "$KUBECONFIG" || true
                            # ArgoCD d'abord (sinon il recree l'application), puis monitoring et namespaces
                            kubectl -n argocd delete application station-ski --ignore-not-found --timeout=120s || true
                            helm uninstall monitoring -n monitoring || true
                            kubectl delete namespace monitoring --ignore-not-found --timeout=300s || true
                            kubectl delete namespace argocd --ignore-not-found --timeout=300s || true
                            kubectl delete namespace "$K8S_NAMESPACE" --ignore-not-found --timeout=300s || true
                        '''
                        dir("${TERRAFORM_DIR}") {
                            sh 'mkdir -p "$TF_PLUGIN_CACHE_DIR"'
                            retry(3) { sh 'terraform init -input=false' }
                            sh 'terraform destroy -input=false -auto-approve -state="$TF_STATE"'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline terminé avec succès.'
        }
        failure {
            echo 'Pipeline en échec : consulter les logs.'
        }
        always {
            sh 'rm -f "$WORKSPACE/settings-nexus.xml" "$WORKSPACE/kubeconfig-eks" || true'
        }
    }
}