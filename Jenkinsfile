/* ============================================================
   FONCTIONS UTILITAIRES
   ============================================================ */

// Fournit les identifiants AWS temporaires (AWS Academy)
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

// true si le dossier a change depuis le dernier commit
// effectivement publie sur Docker Hub (ou si aucun etat memorise)
def changedSince(String dir, String head) {

    def last = sh(
        script: "cat \"\$BUILD_STATE_DIR/${dir}.commit\" 2>/dev/null || true",
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
            description: 'apply = build + EKS + deploiement | plan = jusqu au terraform plan | destroy = supprime EKS'
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
        NEXUS_RELEASES = 'http://nexus:8081/repository/maven-releases/'

        // Repertoires
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

        // Detection des changements
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
           01. SCM CHECKOUT
           ---------------------------------------------------- */

        stage('01 - SCM Checkout (Git)') {

            steps {
                checkout scm
            }
        }


        /* ----------------------------------------------------
           02. CHANGE DETECTION
           ---------------------------------------------------- */

        stage('02 - Change Detection (git diff, incremental build)') {

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
           03. MAVEN CLEAN
           ---------------------------------------------------- */

        stage('03 - Maven Clean') {

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
           04. MAVEN COMPILE AND PACKAGE
           ---------------------------------------------------- */

        stage('04 - Maven Compile and Package (JAR)') {

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
           05. TESTS
           ---------------------------------------------------- */

        stage('05 - Unit and Integration Tests (JUnit 5, Mockito, Testcontainers)') {

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
           06. STATIC CODE ANALYSIS
           ---------------------------------------------------- */

        stage('06 - Static Code Analysis (SonarQube, JaCoCo coverage)') {

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
           07. NEXUS SNAPSHOT
           ---------------------------------------------------- */

        stage('07 - Publish SNAPSHOT Artifact (Nexus Maven Snapshots)') {

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
           08. DOCKER BUILD BACKEND
           ---------------------------------------------------- */

        stage('08 - Docker Build (Backend image)') {

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
           09. DOCKER PUSH BACKEND
           ---------------------------------------------------- */

        stage('09 - Docker Push (Backend image, Docker Hub)') {

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

                    // Memorise le commit seulement apres un push reussi
                    sh '''
                        echo "$HEAD_COMMIT" > "$BUILD_STATE_DIR/backend.commit"
                    '''
                }
            }
        }


        /* ----------------------------------------------------
           10. NEXUS RELEASE (backend uniquement)
           Place apres le build Docker : le second jar genere
           ici ne doit pas gener le COPY du Dockerfile.
           ---------------------------------------------------- */

        stage('10 - Publish RELEASE Artifact (Nexus Maven Releases)') {

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
            <id>nexus-releases</id>
            <username>$NEXUS_USER</username>
            <password>$NEXUS_PASS</password>
        </server>
    </servers>
</settings>
EOF
                    '''

                    dir("${BACKEND_DIR}") {

                        sh '''
                            mvn -B versions:set \
                              -DnewVersion="$IMAGE_TAG" \
                              -DgenerateBackupPoms=false

                            mvn -B deploy -DskipTests \
                              -s "$WORKSPACE/settings-nexus.xml" \
                              -DaltDeploymentRepository=nexus-releases::default::$NEXUS_RELEASES
                        '''
                    }
                }
            }

            post {

                always {

                    // Remet le pom.xml en version SNAPSHOT dans le workspace
                    sh 'git checkout -- backend/pom.xml || true'
                }
            }
        }


        /* ----------------------------------------------------
           11. DOCKER BUILD AND PUSH FRONTEND
           ---------------------------------------------------- */

        stage('11 - Docker Build and Push (Frontend image, Nginx)') {

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
           12. TERRAFORM INIT, FMT, VALIDATE
           ---------------------------------------------------- */

        stage('12 - Terraform Init, Fmt and Validate (IaC lint)') {

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
           13. TERRAFORM PLAN
           ---------------------------------------------------- */

        stage('13 - Terraform Plan (EKS infrastructure)') {

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
           14. MANUAL APPROVAL GATE
           ---------------------------------------------------- */

        stage('14 - Manual Approval Gate (cost control)') {

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
           15. TERRAFORM APPLY
           ---------------------------------------------------- */

        stage('15 - Terraform Apply (EKS cluster, node group, EBS CSI)') {

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
           16. KUBERNETES DEPLOY (MYSQL)
           ---------------------------------------------------- */

        stage('16 - Kubernetes Deploy (MySQL, Secret, StorageClass, PVC)') {

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
           17. GITOPS DEPLOYMENT (ARGOCD)
           ---------------------------------------------------- */

        stage('17 - GitOps Deployment (ArgoCD Application sync)') {

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

                            # Interface web en HTTP
                            kubectl -n argocd patch configmap argocd-cmd-params-cm \
                              --type merge \
                              -p '{"data":{"server.insecure":"true"}}'

                            # NodePort 30081
                            kubectl -n argocd patch svc argocd-server \
                              -p '{"spec":{"type":"NodePort","ports":[{"name":"http","port":80,"targetPort":8080,"nodePort":30081}]}}'

                            kubectl -n argocd rollout restart deployment/argocd-server

                            kubectl -n argocd rollout status deployment/argocd-server --timeout=300s

                            kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=300s

                            kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s

                            # Application GitOps (chemin k8s/app du depot)
                            sed \
                              -e "s|IMAGE_PLACEHOLDER|$DEPLOY_IMAGE|g" \
                              -e "s|FRONTEND_PLACEHOLDER|$FRONTEND_DEPLOY_IMAGE|g" \
                              -e "s|REVISION_PLACEHOLDER|$GIT_REVISION|g" \
                              k8s/argocd/application.yaml | kubectl apply -f -

                            kubectl -n argocd annotate application station-ski \
                              argocd.argoproj.io/refresh=hard \
                              --overwrite

                            # Attendre qu'ArgoCD ait deploye la bonne image
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
                              --timeout=600s || {

                                echo "Diagnostic application"

                                kubectl -n "$K8S_NAMESPACE" get pods -o wide

                                kubectl -n "$K8S_NAMESPACE" describe pod -l app=station-ski | tail -40

                                kubectl -n "$K8S_NAMESPACE" logs deploy/station-ski --tail=80 || true

                                kubectl -n "$K8S_NAMESPACE" logs deploy/station-ski --previous --tail=80 || true

                                exit 1
                            }

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
           18. OBSERVABILITY STACK
           ---------------------------------------------------- */

        stage('18 - Observability Stack (Helm, kube-prometheus-stack)') {

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

                                # Repository Helm
                                helm repo add prometheus-community \
                                  https://prometheus-community.github.io/helm-charts

                                helm repo update

                                # Prometheus et Grafana
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
           19. TERRAFORM DESTROY
           ---------------------------------------------------- */

        stage('19 - Terraform Destroy (EKS teardown)') {

            when {
                expression {
                    env.ACTION == 'destroy'
                }
            }

            steps {

                script {

                    withAws {

                        // Supprime d'abord les ressources Kubernetes
                        // (volumes EBS crees par les PVC)
                        sh '''
                            export KUBECONFIG="$WORKSPACE/kubeconfig-eks"

                            aws eks update-kubeconfig \
                              --region "$AWS_DEFAULT_REGION" \
                              --name "$CLUSTER_NAME" \
                              --kubeconfig "$KUBECONFIG" || true

                            # ArgoCD d'abord, sinon il recree l'application
                            kubectl -n argocd delete application station-ski \
                              --ignore-not-found \
                              --timeout=120s || true

                            helm uninstall monitoring -n monitoring || true

                            kubectl delete namespace monitoring \
                              --ignore-not-found \
                              --timeout=300s || true

                            kubectl delete namespace argocd \
                              --ignore-not-found \
                              --timeout=300s || true

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
                to: 'saharsaidani8@gmail.com',

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
                to: 'saharsaidani8@gmail.com',

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
