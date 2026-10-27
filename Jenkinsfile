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

// Compare le hash Git d'un dossier avec celui du dernier build d'image memorise.
// Retourne [changed: true/false, tree: hash du dossier, tag: tag de l'ancienne image]
def detectChange(String dir, String stateFile) {
    def tree = sh(script: "git rev-parse HEAD:${dir}", returnStdout: true).trim()
    def last = sh(script: "cat '${stateFile}' 2>/dev/null || true", returnStdout: true).trim()
    def parts = last ? last.tokenize(' ') : []
    def unchanged = !params.FORCE_BUILD && parts.size() == 2 && parts[0] == tree
    return [changed: !unchanged, tree: tree, tag: unchanged ? parts[1] : '']
}

pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    parameters {
        choice(name: 'ACTION',
               choices: ['apply', 'plan', 'destroy'],
               description: 'apply = build + EKS + deploiement | plan = jusqu au terraform plan | destroy = supprime EKS')
        booleanParam(name: 'FORCE_BUILD',
                     defaultValue: false,
                     description: 'Forcer le rebuild de l image meme si le code backend n a pas change (backend et frontend)')
    }

    environment {
        ACTION             = "${params.ACTION ?: 'apply'}"

        registry           = 'saharsaidani/gestion-station-skii'
        registryCredential = 'dockerhub'
        frontendRegistry   = 'saharsaidani/gestion-station-skii-frontend'
        dockerImage        = ''

        SONAR_URL          = 'http://sonarqube:9000'
        NEXUS_SNAPSHOTS    = 'http://nexus:8081/repository/maven-snapshots/'
        BACKEND_DIR        = 'backend'
        FRONTEND_DIR       = 'frontend'
        TERRAFORM_DIR      = 'terraform'
        TF_IN_AUTOMATION   = 'true'
        TF_PLUGIN_CACHE_DIR = '/var/jenkins_home/.terraform.d/plugin-cache'
        // Etat Terraform conserve hors du workspace
        TF_STATE           = '/var/jenkins_home/tfstate/stationski.tfstate'
        // Dernier build du backend : "<hash du dossier backend> <tag de l'image>"
        BUILD_STATE        = '/var/jenkins_home/buildstate/backend-image.txt'
        // Dernier build du frontend : "<hash du dossier frontend> <tag de l'image>"
        FRONTEND_BUILD_STATE = '/var/jenkins_home/buildstate/frontend-image.txt'

        AWS_DEFAULT_REGION = 'us-east-1'
        CLUSTER_NAME       = 'stationski'
        K8S_NAMESPACE      = 'station-ski'
        GIT_REVISION       = 'feature/sahar-saidani'

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

        stage('CHECKOUT GIT') {
            steps {
                checkout scm
            }
        }

        // Compare le contenu des dossiers backend et frontend avec ceux du dernier build d'image.
        // Si identique : on saute le build et on reutilise l'image deja publiee.
        stage('DETECT CHANGES') {
            when { expression { env.ACTION != 'destroy' } }
            steps {
                script {
                    def be = detectChange(env.BACKEND_DIR, env.BUILD_STATE)
                    env.BACKEND_TREE = be.tree
                    env.BACKEND_CHANGED = be.changed.toString()
                    if (!be.changed) {
                        env.IMAGE_TAG = be.tag
                        echo "Backend inchange -> reutilisation de ${registry}:${env.IMAGE_TAG}"
                    } else {
                        echo "Backend modifie (ou premier build / FORCE_BUILD) -> nouvelle image"
                    }

                    def fe = detectChange(env.FRONTEND_DIR, env.FRONTEND_BUILD_STATE)
                    env.FRONTEND_TREE = fe.tree
                    env.FRONTEND_CHANGED = fe.changed.toString()
                    if (!fe.changed) {
                        env.FRONTEND_TAG = fe.tag
                        echo "Frontend inchange -> reutilisation de ${frontendRegistry}:${env.FRONTEND_TAG}"
                    } else {
                        echo "Frontend modifie (ou premier build / FORCE_BUILD) -> nouvelle image"
                    }
                }
            }
        }

        stage('MVN CLEAN') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B clean'
                }
            }
        }

        stage('ARTIFACT CONSTRUCTION') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B package -DskipTests'
                }
            }
        }

        stage('UNIT TESTS') {
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

        stage('MVN SONARQUBE') {
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

        stage('PUBLISH TO NEXUS') {
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

        stage('BUILDING OUR IMAGE') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                script {
                    def pomVersion = sh(
                        script: "cd ${BACKEND_DIR} && mvn -q -B help:evaluate -Dexpression=project.version -DforceStdout",
                        returnStdout: true
                    ).trim()
                    env.IMAGE_TAG = "${pomVersion.replace('-SNAPSHOT', '')}-${BUILD_NUMBER}"
                    echo "Image : ${registry}:${env.IMAGE_TAG}"
                    dockerImage = docker.build("${registry}:${env.IMAGE_TAG}", "${BACKEND_DIR}")
                }
            }
        }

        stage('DEPLOY OUR IMAGE') {
            when { expression { env.ACTION != 'destroy' && env.BACKEND_CHANGED == 'true' } }
            steps {
                script {
                    docker.withRegistry('', registryCredential) {
                        retry(3) { dockerImage.push() }
                        retry(3) { dockerImage.push('latest') }
                    }
                    // Memorise le build seulement apres un push reussi
                    sh '''
                        mkdir -p "$(dirname "$BUILD_STATE")"
                        echo "$BACKEND_TREE $IMAGE_TAG" > "$BUILD_STATE"
                    '''
                }
            }
        }

        // Le build Angular se fait dans le Dockerfile (multi-stage) : pas besoin de Node sur Jenkins
        stage('FRONTEND IMAGE') {
            when { expression { env.ACTION != 'destroy' && env.FRONTEND_CHANGED == 'true' } }
            steps {
                script {
                    env.FRONTEND_TAG = "front-${BUILD_NUMBER}"
                    echo "Image : ${frontendRegistry}:${env.FRONTEND_TAG}"
                    def feImage = docker.build("${frontendRegistry}:${env.FRONTEND_TAG}", "${FRONTEND_DIR}")
                    docker.withRegistry('', registryCredential) {
                        retry(3) { feImage.push() }
                        retry(3) { feImage.push('latest') }
                    }
                    // Memorise le build seulement apres un push reussi
                    sh '''
                        mkdir -p "$(dirname "$FRONTEND_BUILD_STATE")"
                        echo "$FRONTEND_TREE $FRONTEND_TAG" > "$FRONTEND_BUILD_STATE"
                    '''
                }
            }
        }

        stage('TERRAFORM VALIDATE') {
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

        stage('TERRAFORM PLAN') {
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

        stage('APPROVAL') {
            when { expression { env.ACTION == 'apply' || env.ACTION == 'destroy' } }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "Confirmer terraform ${env.ACTION} sur EKS (ressources facturees) ?",
                          ok: 'Confirmer'
                }
            }
        }

        stage('TERRAFORM APPLY') {
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

        stage('KUBERNETES') {
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

        stage('ARGOCD') {
            when { expression { env.ACTION == 'apply' } }
            steps {
                script {
                    env.DEPLOY_IMAGE = "${registry}:${env.IMAGE_TAG ?: 'latest'}"
                    env.FRONTEND_DEPLOY_IMAGE = "${frontendRegistry}:${env.FRONTEND_TAG ?: 'latest'}"
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
                                -e "s|FRONTEND_PLACEHOLDER|$FRONTEND_DEPLOY_IMAGE|g" \
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

                            # Meme verification pour le frontend
                            CURF=""
                            for i in $(seq 1 120); do
                                CURF=$(kubectl -n "$K8S_NAMESPACE" get deploy station-ski-frontend \
                                       -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                                [ "$CURF" = "$FRONTEND_DEPLOY_IMAGE" ] && break
                                sleep 5
                            done
                            [ "$CURF" = "$FRONTEND_DEPLOY_IMAGE" ] || {
                                echo "ArgoCD n'a pas synchronise l'image frontend attendue ($FRONTEND_DEPLOY_IMAGE)"
                                kubectl -n argocd describe application station-ski | tail -40
                                exit 1
                            }
                            kubectl -n "$K8S_NAMESPACE" rollout status deployment/station-ski-frontend --timeout=300s
                            kubectl -n argocd get applications
                            kubectl -n "$K8S_NAMESPACE" get pods,svc

                            NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
                            echo "=========================================================="
                            echo "Frontend    : http://$NODE_IP:30082"
                            echo "Backend     : http://$NODE_IP:30080/stationSki/swagger-ui/index.html"
                            echo "ArgoCD      : http://$NODE_IP:30081  (utilisateur admin)"
                            echo "Mot de passe ArgoCD : kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d"
                            echo "=========================================================="
                        '''
                    }
                }
            }
        }

        stage('MONITORING') {
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

        stage('TERRAFORM DESTROY') {
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