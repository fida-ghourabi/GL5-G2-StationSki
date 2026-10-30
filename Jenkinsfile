
/* ============================================================
   FONCTIONS UTILITAIRES
   ============================================================ */

// Fournir les identifiants AWS temporaires
def withAws(Closure body) {
    withCredentials([
        string(credentialsId: 'aws-access-key',
               variable: 'AWS_ACCESS_KEY_ID'),

        string(credentialsId: 'aws-secret-key',
               variable: 'AWS_SECRET_ACCESS_KEY'),

        string(credentialsId: 'aws-session-token',
               variable: 'AWS_SESSION_TOKEN')
    ]) {
        body()
    }
}

// Vérifier si un dossier a changé depuis le dernier commit
// effectivement publié sur Docker Hub.
def changedSince(String dir, String head) {

    def last = sh(
        script: "cat \"$BUILD_STATE_DIR/${dir}.commit\" 2>/dev/null || true",
        returnStdout: true
    ).trim()

    if (!last) {
        return true
    }

    return sh(
        script: "git diff --quiet ${last} ${head} -- ${dir}",
        returnStatus: true
    ) != 0
}


/* ============================================================
   PIPELINE PRINCIPAL
   ============================================================ */

pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    parameters {

        booleanParam(
            name: 'FORCE_BUILD',
            defaultValue: false,
            description: 'Forcer le build Backend et Frontend'
        )

        choice(
            name: 'ACTION',
            choices: ['apply', 'plan', 'destroy'],
            description: 'Action Terraform et Kubernetes'
        )
    }

    environment {

        // ACTION
        ACTION = "${params.ACTION ?: 'apply'}"

        // Docker Hub
        registry = 'saharsaidani/gestion-station-skii'
        FRONTEND_REGISTRY = 'saharsaidani/gestion-station-skii-frontend'
        registryCredential = 'dockerhub'

        // Outils
        SONAR_URL = 'http://sonarqube:9000'
        NEXUS_SNAPSHOTS = 'http://nexus:8081/repository/maven-snapshots/'

        // Répertoires
        BACKEND_DIR = 'backend'
        TERRAFORM_DIR = 'terraform'

        // Terraform
        TF_IN_AUTOMATION = 'true'
        TF_PLUGIN_CACHE_DIR = '/var/jenkins_home/.terraform.d/plugin-cache'
        TF_STATE = '/var/jenkins_home/tfstate/stationski.tfstate'

        // AWS / EKS
        AWS_DEFAULT_REGION = 'us-east-1'
        CLUSTER_NAME = 'stationski'
        NODEGROUP_NAME = 'stationski-nodes'

        // Kubernetes
        K8S_NAMESPACE = 'station-ski'

        // GitOps
        GIT_REVISION = 'feature/sahar-saidani'

        // Détection des changements
        BUILD_STATE_DIR = '/var/jenkins_home/build-state'

        // Tests
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'

        TEST_DB_URL = 'jdbc:mysql://mysql:3306/station_test?createDatabaseIfNotExist=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8'
    }

    options {

        timestamps()

        disableConcurrentBuilds()

        timeout(
            time: 120,
            unit: 'MINUTES'
        )

        buildDiscarder(
            logRotator(numToKeepStr: '10')
        )
    }


    /* ========================================================
       STAGES
       ======================================================== */

    stages {


        /* ----------------------------------------------------
           1. GIT
           ---------------------------------------------------- */

        stage('1 - Recuperation du code Git') {

            steps {
                checkout scm
            }
        }


        /* ----------------------------------------------------
           2. DETECTION DES CHANGEMENTS
           ---------------------------------------------------- */

        stage('2 - Detection des changements') {

            when {
                expression {
                    env.ACTION != 'destroy'
                }
            }

            steps {

                script {

                    sh 'mkdir -p "$BUILD_STATE_DIR"'

                    env.HEAD_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()

                    def backend = true
                    def frontend = true

                    if (!params.FORCE_BUILD) {

                        backend = changedSince(
                            'backend',
                            env.HEAD_COMMIT
                        )

                        frontend = changedSince(
                            'frontend',
                            env.HEAD_COMMIT
                        )
                    }

                    env.BACKEND_CHANGED = backend.toString()
                    env.FRONTEND_CHANGED = frontend.toString()

                    echo """
                    Commit : ${env.HEAD_COMMIT}
                    Backend modifie : ${env.BACKEND_CHANGED}
                    Frontend modifie : ${env.FRONTEND_CHANGED}
                    FORCE_BUILD : ${params.FORCE_BUILD}
                    """
                }
            }
        }


        /* ----------------------------------------------------
           3. CLEAN MAVEN
           ---------------------------------------------------- */

        stage('3 - Nettoyage Maven') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                dir("${BACKEND_DIR}") {

                    sh 'mvn -B clean'
                }
            }
        }


        /* ----------------------------------------------------
           4. BUILD BACKEND
           ---------------------------------------------------- */

        stage('4 - Compilation et package JAR') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                dir("${BACKEND_DIR}") {

                    sh 'mvn -B package -DskipTests'
                }
            }
        }


        /* ----------------------------------------------------
           5. TESTS UNITAIRES
           ---------------------------------------------------- */

        stage('5 - Tests JUnit et Mockito') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'mysql-test',
                        usernameVariable: 'TEST_DB_USERNAME',
                        passwordVariable: 'TEST_DB_PASSWORD'
                    )
                ]) {

                    dir("${BACKEND_DIR}") {

                        sh 'mvn -B test'
                    }
                }
            }

            post {

                always {

                    junit(
                        allowEmptyResults: true,
                        testResults: 'backend/target/surefire-reports/TEST-*.xml'
                    )
                }
            }
        }


        /* ----------------------------------------------------
           6. SONARQUBE
           ---------------------------------------------------- */

        stage('6 - Analyse qualite SonarQube') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                withCredentials([
                    string(
                        credentialsId: 'sonar-token',
                        variable: 'SONAR_TOKEN'
                    )
                ]) {

                    dir("${BACKEND_DIR}") {

                        sh '''
                            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.8.0.7211:sonar \
                              -Dsonar.projectKey=gestion-station-skii \
                              -Dsonar.host.url="$SONAR_URL" \
                              -Dsonar.token="$SONAR_TOKEN" \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }


        /* ----------------------------------------------------
           7. NEXUS
           ---------------------------------------------------- */

        stage('7 - Publication du JAR sur Nexus') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-creds',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {

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
                }
            }
        }


        /* ----------------------------------------------------
           8. DOCKER BACKEND BUILD
           ---------------------------------------------------- */

        stage('8 - Build image Docker Backend') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                script {

                    def pomVersion = sh(
                        script: """
                            cd ${BACKEND_DIR} &&
                            mvn -q -B help:evaluate \
                            -Dexpression=project.version \
                            -DforceStdout
                        """,
                        returnStdout: true
                    ).trim()

                    env.IMAGE_TAG =
                        "${pomVersion.replace('-SNAPSHOT', '')}-${BUILD_NUMBER}"

                    echo "Backend image : ${registry}:${env.IMAGE_TAG}"

                    docker.build(
                        "${registry}:${env.IMAGE_TAG}",
                        "${BACKEND_DIR}"
                    )
                }
            }
        }


        /* ----------------------------------------------------
           9. PUSH BACKEND
           ---------------------------------------------------- */

        stage('9 - Push image Backend Docker Hub') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.BACKEND_CHANGED == 'true'
                }
            }

            steps {

                script {

                    def backendImage = docker.image(
                        "${registry}:${env.IMAGE_TAG}"
                    )

                    docker.withRegistry(
                        '',
                        registryCredential
                    ) {

                        retry(3) {
                            backendImage.push()
                        }

                        retry(3) {
                            backendImage.push('latest')
                        }
                    }

                    sh '''
                        echo "$HEAD_COMMIT" > "$BUILD_STATE_DIR/backend.commit"
                    '''
                }
            }
        }


        /* ----------------------------------------------------
           10. FRONTEND BUILD + PUSH
           ---------------------------------------------------- */

        stage('10 - Build et Push image Frontend') {

            when {
                expression {
                    env.ACTION != 'destroy' &&
                    env.FRONTEND_CHANGED == 'true'
                }
            }

            steps {

                script {

                    env.FRONTEND_TAG = "front-${BUILD_NUMBER}"

                    def frontImage =
                        "${FRONTEND_REGISTRY}:${env.FRONTEND_TAG}"

                    echo "Frontend image : ${frontImage}"

                    retry(3) {
                        sh "docker build -t ${frontImage} frontend"
                    }

                    docker.withRegistry(
                        '',
                        registryCredential
                    ) {

                        retry(3) {
                            sh "docker push ${frontImage}"
                        }

                        sh """
                            docker tag ${frontImage} ${FRONTEND_REGISTRY}:latest
                        """

                        retry(3) {
                            sh "docker push ${FRONTEND_REGISTRY}:latest"
                        }
                    }

                    sh '''
                        echo "$HEAD_COMMIT" > "$BUILD_STATE_DIR/frontend.commit"
                    '''
                }
            }
        }


        /* ----------------------------------------------------
           11. TERRAFORM VALIDATION
           ---------------------------------------------------- */

        stage('11 - Terraform Validation') {

            when {
                expression {
                    env.ACTION != 'destroy'
                }
            }

            steps {

                dir("${TERRAFORM_DIR}") {

                    sh 'terraform version'

                    catchError(
                        buildResult: 'SUCCESS',
                        stageResult: 'UNSTABLE'
                    ) {

                        sh 'terraform fmt -check -recursive -diff'
                    }

                    sh 'mkdir -p "$TF_PLUGIN_CACHE_DIR"'

                    retry(3) {

                        sh 'terraform init -input=false'
                    }

                    sh 'terraform validate'
                }
            }
        }


        /* ----------------------------------------------------
           12. TERRAFORM PLAN
           ---------------------------------------------------- */

        stage('12 - Terraform Plan') {

            when {
                expression {
                    env.ACTION == 'apply' ||
                    env.ACTION == 'plan'
                }
            }

            steps {

                script {

                    withAws {

                        dir("${TERRAFORM_DIR}") {

                            sh '''
                                mkdir -p "$TF_PLUGIN_CACHE_DIR" "$(dirname "$TF_STATE")"

                                terraform init -input=false

                                terraform plan \
                                  -input=false \
                                  -state="$TF_STATE"
                            '''
                        }
                    }
                }
            }
        }


        /* ----------------------------------------------------
           13. VALIDATION MANUELLE
           ---------------------------------------------------- */

        stage('13 - Validation manuelle') {

            when {
                expression {
                    env.ACTION == 'apply' ||
                    env.ACTION == 'destroy'
                }
            }

            steps {

                timeout(
                    time: 30,
                    unit: 'MINUTES'
                ) {

                    input(
                        message: "Confirmer Terraform ${env.ACTION} sur AWS EKS ? Ressources facturees.",
                        ok: 'Confirmer'
                    )
                }
            }
        }


        /* ----------------------------------------------------
           14. TERRAFORM APPLY
           ---------------------------------------------------- */

        stage('14 - Creation du cluster EKS') {

            when {
                expression {
                    env.ACTION == 'apply'
                }
            }

            steps {

                script {

                    withAws {

                        dir("${TERRAFORM_DIR}") {

                            sh '''
                                terraform apply \
                                  -input=false \
                                  -auto-approve \
                                  -state="$TF_STATE"
                            '''
                        }
                    }
                }
            }
        }


        /* ----------------------------------------------------
           15. MYSQL ON EKS
           ---------------------------------------------------- */

        stage('15 - Deploiement MySQL sur EKS') {

            when {
                expression {
                    env.ACTION == 'apply'
                }
            }

            steps {

                script {

                    env.DEPLOY_IMAGE =
                        "${registry}:${env.IMAGE_TAG ?: 'latest'}"

                    withAws {

                        withCredentials([

                            usernamePassword(
                                credentialsId: 'mysql-app',
                                usernameVariable: 'DB_USER',
                                passwordVariable: 'DB_PASS'
                            ),

                            string(
                                credentialsId: 'mysql-root-password',
                                variable: 'DB_ROOT_PASS'
                            )
                        ]) {

                            sh '''
                                set -e

                                export KUBECONFIG="$WORKSPACE/kubeconfig-eks"

                                aws eks update-kubeconfig \
                                  --region "$AWS_DEFAULT_REGION" \
                                  --name "$CLUSTER_NAME" \
                                  --kubeconfig "$KUBECONFIG"

                                # Attendre le node group AWS
                                aws eks wait nodegroup-active \
                                  --cluster-name "$CLUSTER_NAME" \
                                  --nodegroup-name "$NODEGROUP_NAME" \
                                  --region "$AWS_DEFAULT_REGION"

                                # Attendre les noeuds Kubernetes
                                N=0

                                for i in $(seq 1 60); do

                                    N=$(kubectl get nodes --no-headers 2>/dev/null | wc -l)

                                    [ "$N" -gt 0 ] && break

                                    echo "En attente des noeuds ($i/60)..."

                                    sleep 10
                                done

                                if [ "$N" -eq 0 ]; then

                                    echo "Aucun noeud Kubernetes disponible"

                                    aws eks describe-nodegroup \
                                      --cluster-name "$CLUSTER_NAME" \
                                      --nodegroup-name "$NODEGROUP_NAME" \
                                      --region "$AWS_DEFAULT_REGION"

                                    exit 1
                                fi

                                kubectl wait \
                                  --for=condition=Ready \
                                  nodes --all \
                                  --timeout=600s

                                kubectl get nodes -o wide

                                # Namespace
                                kubectl create namespace "$K8S_NAMESPACE" \
                                  --dry-run=client -o yaml | kubectl apply -f -

                                # Secrets MySQL
                                kubectl -n "$K8S_NAMESPACE" create secret generic mysql-credentials \
                                  --from-literal=MYSQL_USER="$DB_USER" \
                                  --from-literal=MYSQL_PASSWORD="$DB_PASS" \
                                  --from-literal=MYSQL_ROOT_PASSWORD="$DB_ROOT_PASS" \
                                  --from-literal=MYSQL_DATABASE=skidb \
                                  --dry-run=client -o yaml | kubectl apply -f -

                                # Stockage et MySQL
                                kubectl apply -f k8s/storageclass.yaml

                                kubectl apply -f k8s/mysql.yaml

                                # Attendre MySQL
                                kubectl -n "$K8S_NAMESPACE" rollout status \
                                  deployment/mysql \
                                  --timeout=300s || {

                                    echo "Diagnostic MySQL"

                                    kubectl -n "$K8S_NAMESPACE" get pods,pvc -o wide

                                    kubectl -n "$K8S_NAMESPACE" describe pvc mysql-data || true

                                    kubectl -n "$K8S_NAMESPACE" describe pod -l app=mysql || true

                                    kubectl -n "$K8S_NAMESPACE" logs -l app=mysql --tail=40 || true

                                    exit 1
                                }

                                kubectl -n "$K8S_NAMESPACE" get pods,pvc
                            '''
                        }
                    }
                }
            }
        }


        /* ----------------------------------------------------
           16. ARGOCD GITOPS
           ---------------------------------------------------- */

        stage('16 - ArgoCD Deploiement GitOps') {

            when {
                expression {
                    env.ACTION == 'apply'
                }
            }

            steps {

                script {

                    env.DEPLOY_IMAGE =
                        "${registry}:${env.IMAGE_TAG ?: 'latest'}"

                    env.FRONTEND_DEPLOY_IMAGE =
                        "${FRONTEND_REGISTRY}:${env.FRONTEND_TAG ?: 'latest'}"

                    withAws {

                        sh '''
                            set -e

                            export KUBECONFIG="$WORKSPACE/kubeconfig-eks"

                            aws eks update-kubeconfig \
                              --region "$AWS_DEFAULT_REGION" \
                              --name "$CLUSTER_NAME" \
                              --kubeconfig "$KUBECONFIG"

                            # Installer ArgoCD
                            kubectl create namespace argocd \
                              --dry-run=client -o yaml | kubectl apply -f -

                            kubectl apply -n argocd \
                              --server-side \
                              --force-conflicts \
                              -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

                            kubectl wait \
                              --for=condition=Established \
                              crd/applications.argoproj.io \
                              --timeout=120s

                            # Configuration HTTP
                            kubectl -n argocd patch configmap argocd-cmd-params-cm \
                              --type merge \
                              -p '{"data":{"server.insecure":"true"}}'

                            # NodePort
                            kubectl -n argocd patch svc argocd-server \
                              -p '{"spec":{"type":"NodePort","ports":[{"name":"http","port":80,"targetPort":8080,"nodePort":30081}]}}'

                            kubectl -n argocd rollout restart deployment/argocd-server

                            kubectl -n argocd rollout status deployment/argocd-server --timeout=300s

                            kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=300s

                            kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s

                            # Application GitOps
                            sed \
                              -e "s|IMAGE_PLACEHOLDER|$DEPLOY_IMAGE|g" \
                              -e "s|FRONTEND_PLACEHOLDER|$FRONTEND_DEPLOY_IMAGE|g" \
                              -e "s|REVISION_PLACEHOLDER|$GIT_REVISION|g" \
                              k8s/argocd/application.yaml | kubectl apply -f -

                            kubectl -n argocd annotate application station-ski \
                              argocd.argoproj.io/refresh=hard \
                              --overwrite

                            # Attendre la synchronisation
                            CUR=""

                            for i in $(seq 1 60); do

                                CUR=$(kubectl -n "$K8S_NAMESPACE" get deploy station-ski \
                                    -o jsonpath='{.spec.template.spec.containers[0].image}' \
                                    2>/dev/null || true)

                                [ "$CUR" = "$DEPLOY_IMAGE" ] && break

                                sleep 5
                            done

                            if [ "$CUR" != "$DEPLOY_IMAGE" ]; then

                                echo "ArgoCD n'a pas synchronise l'image attendue"

                                kubectl -n argocd get applications

                                kubectl -n argocd describe application station-ski

                                exit 1
                            fi

                            kubectl -n "$K8S_NAMESPACE" rollout status \
                              deployment/station-ski \
                              --timeout=600s

                            kubectl -n argocd get applications

                            kubectl -n "$K8S_NAMESPACE" get pods,svc

                            # Adresse publique
                            NODE_IP=$(kubectl get nodes \
                              -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')

                            echo "============================================"
                            echo "Application : http://$NODE_IP:30080/stationSki/swagger-ui/index.html"
                            echo "ArgoCD      : http://$NODE_IP:30081"
                            echo "============================================"
                        '''
                    }
                }
            }
        }


        /* ----------------------------------------------------
           17. MONITORING
           ---------------------------------------------------- */

        stage('17 - Monitoring Prometheus et Grafana') {

            when {
                expression {
                    env.ACTION == 'apply'
                }
            }

            steps {

                script {

                    withAws {

                        withCredentials([

                            string(
                                credentialsId: 'grafana-admin-password',
                                variable: 'GRAFANA_PASS'
                            )
                        ]) {

                            sh '''
                                set -e

                                export KUBECONFIG="$WORKSPACE/kubeconfig-eks"

                                aws eks update-kubeconfig \
                                  --region "$AWS_DEFAULT_REGION" \
                                  --name "$CLUSTER_NAME" \
                                  --kubeconfig "$KUBECONFIG"

                                # Ajouter le repository Helm
                                helm repo add prometheus-community \
                                  https://prometheus-community.github.io/helm-charts

                                helm repo update

                                # Installer Prometheus et Grafana
                                helm upgrade --install monitoring \
                                  prometheus-community/kube-prometheus-stack \
                                  --namespace monitoring \
                                  --create-namespace \
                                  -f k8s/monitoring/values.yaml \
                                  --set grafana.adminPassword="$GRAFANA_PASS" \
                                  --wait \
                                  --timeout 15m

                                kubectl -n monitoring get pods

                                NODE_IP=$(kubectl get nodes \
                                  -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')

                                echo "============================================"
                                echo "Grafana : http://$NODE_IP:30300"
                                echo "Utilisateur : admin"
                                echo "============================================"
                            '''
                        }
                    }
                }
            }
        }


        /* ----------------------------------------------------
           18. TERRAFORM DESTROY
           ---------------------------------------------------- */

        stage('18 - Destruction du cluster EKS') {

            when {
                expression {
                    env.ACTION == 'destroy'
                }
            }

            steps {

                script {

                    withAws {

                        sh '''
                            export KUBECONFIG="$WORKSPACE/kubeconfig-eks"

                            # Charger la configuration EKS
                            aws eks update-kubeconfig \
                              --region "$AWS_DEFAULT_REGION" \
                              --name "$CLUSTER_NAME" \
                              --kubeconfig "$KUBECONFIG" || true

                            # Supprimer les applications GitOps
                            kubectl -n argocd delete application station-ski \
                              --ignore-not-found \
                              --timeout=120s || true

                            # Supprimer le monitoring
                            helm uninstall monitoring -n monitoring || true

                            kubectl delete namespace monitoring \
                              --ignore-not-found \
                              --timeout=300s || true

                            # Supprimer ArgoCD
                            kubectl delete namespace argocd \
                              --ignore-not-found \
                              --timeout=300s || true

                            # Supprimer MySQL et ses PVC
                            kubectl delete namespace "$K8S_NAMESPACE" \
                              --ignore-not-found \
                              --timeout=300s || true
                        '''

                        dir("${TERRAFORM_DIR}") {

                            sh '''
                                mkdir -p "$TF_PLUGIN_CACHE_DIR"

                                terraform init -input=false

                                terraform destroy \
                                  -input=false \
                                  -auto-approve \
                                  -state="$TF_STATE"
                            '''
                        }
                    }
                }
            }
        }
    }


    /* ========================================================
       POST ACTIONS
       ======================================================== */

    post {

        success {

            emailext(
                to: 'ton.email@gmail.com',

                subject: "SUCCES : ${env.JOB_NAME} #${env.BUILD_NUMBER}",

                body: """
Le pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}
s'est termine avec succes.

Action : ${env.ACTION}

Details :
${env.BUILD_URL}
"""
            )
        }

        failure {

            emailext(
                to: 'ton.email@gmail.com',

                subject: "ECHEC : ${env.JOB_NAME} #${env.BUILD_NUMBER}",

                body: """
Le pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}
a echoue.

Action : ${env.ACTION}

Logs :
${env.BUILD_URL}console
""",

                attachLog: true
            )
        }

        always {

            sh '''
                rm -f "$WORKSPACE/settings-nexus.xml"
                rm -f "$WORKSPACE/kubeconfig-eks"
            '''
        }
    }
}
