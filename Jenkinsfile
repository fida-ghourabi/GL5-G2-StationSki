pipeline {
    /*
     * Jenkins utilise un agent capable d'exécuter :
     * - Maven
     * - Java
     * - Docker
     */
    agent any

    /*
     * Reconstruction complète de l'infrastructure, uniquement sur demande.
     * La suppression exige aussi une approbation manuelle dans le stage dédié.
     */
    parameters {
        booleanParam(
            name: 'DESTROY_AWS_INFRASTRUCTURE_ONLY',
            defaultValue: false,
            description: 'Supprimer toutes les ressources Terraform AWS et arrêter le pipeline sans les recréer.'
        )
    }

    /*
     * Options générales du pipeline.
     */
    options {
        /*
         * Désactive le checkout automatique de Jenkins.
         * Le checkout est réalisé explicitement dans CHECKOUT GIT.
         */
        skipDefaultCheckout(true)

        /*
         * Empêche deux builds du même job de s'exécuter en parallèle.
         */
        disableConcurrentBuilds()

        /*
         * Ajoute l'heure dans les logs Jenkins.
         */
        timestamps()

        /*
         * Conserve uniquement les 10 derniers builds
         * et les artefacts associés.
         */
        buildDiscarder(
            logRotator(
                numToKeepStr: '10',
                artifactNumToKeepStr: '10'
            )
        )
    }

    /*
     * Outils configurés dans :
     * Manage Jenkins → Tools
     *
     * Les noms doivent être exactement :
     * - JDK17
     * - Maven
     */
    tools {
        jdk 'JDK17'
        maven 'Maven'
    }

    /*
     * Variables globales du pipeline.
     */
    environment {
        /*
         * Nom complet du repository Docker Hub.
         * Remplacez fidagh par votre véritable utilisateur Docker Hub
         * si nécessaire.
         */
        DOCKER_IMAGE = 'fidagh/gestion-station-skii-backend'

        /*
         * BUILD_NUMBER est fourni automatiquement par Jenkins.
         * Exemple : 15
         */
        IMAGE_TAG = "${BUILD_NUMBER}"

        /*
         * URL de Nexus accessible depuis le conteneur Jenkins.
         *
         * Jenkins et Nexus doivent être connectés
         * au même réseau Docker.
         */
        NEXUS_URL = 'http://nexus:8081'
        FRONTEND_NEXUS_REPOSITORY = 'frontend-releases'

        /*
         * Nom du serveur SonarQube configuré dans Jenkins.
         */
        SONARQUBE_SERVER = 'SonarQube'

        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'

        AWS_REGION = 'us-east-1'
        TERRAFORM_DIR = 'infrastructure/terraform'

    }

    stages {

        /*
         * ============================================================
         * Stage 1 : CHECKOUT GIT
         * ============================================================
         *
         * Récupère le code de la branche configurée
         * dans le job Jenkins.
         */
        stage('CHECKOUT GIT') {
            steps {
                /*
                 * checkout scm utilise automatiquement :
                 * - l'URL Git configurée dans Jenkins ;
                 * - la branche sélectionnée ;
                 * - les credentials Git configurés.
                 */
                checkout scm
            }
        }

        /*
         * ============================================================
         * Stage 2 : MVN CLEAN
         * ============================================================
         *
         * Supprime les anciens fichiers compilés dans :
         *
         * backend/target/
         *
         * Cela évite d'utiliser un ancien JAR ou d'anciens fichiers.
         */
        stage('MVN CLEAN') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                /*
                 * Le projet Maven se trouve dans backend.
                 */
                dir('backend') {
                    /*
                     * Utilise Maven configuré dans Jenkins.
                     */
                    sh 'mvn clean'
                }
            }
        }

        /*
         * ============================================================
         * Stage 3 : ARTIFACT CONSTRUCTION
         * ============================================================
         *
         * Compile le projet et crée le fichier JAR.
         *
         * Les tests ne sont pas exécutés ici, car ils disposent
         * d'un stage dédié UNIT TESTS.
         */
        stage('ARTIFACT CONSTRUCTION') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                dir('backend') {
                    /*
                     * package :
                     * - compile le code Java ;
                     * - génère les classes ;
                     * - crée le fichier JAR.
                     *
                     * -DskipTests évite d'exécuter les tests
                     * dans cette étape.
                     */
                    sh 'mvn package -DskipTests'
                }
            }

            post {
                success {
                    /*
                     * Archive le JAR dans Jenkins afin de pouvoir
                     * le télécharger depuis la page du build.
                     */
                    archiveArtifacts(
                        artifacts: 'backend/target/*.jar',
                        fingerprint: true
                    )
                }
            }
        }

        /*
         * ============================================================
         * Stage 4 : UNIT TESTS
         * ============================================================
         *
         * Exécute les tests Maven.
         */
        stage('UNIT TESTS') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                dir('backend') {
                    /*
                     * Si un test échoue, Maven retourne une erreur
                     * et le pipeline devient rouge.
                     */
                    sh 'mvn test'
                }
            }

            post {
                always {
                    /*
                     * Publie les rapports JUnit dans Jenkins.
                     *
                     * allowEmptyResults: false signifie que le pipeline
                     * signale une erreur si aucun rapport JUnit n'est trouvé.
                     */
                    junit(
                        testResults: 'backend/target/surefire-reports/*.xml',
                        allowEmptyResults: false
                    )
                }
            }
        }

        /*
         * ============================================================
         * Stage 5 : MVN SONARQUBE
         * ============================================================
         *
         * Envoie le code source et le rapport de couverture
         * vers SonarQube.
         */
        stage('MVN SONARQUBE') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                /*
                 * withSonarQubeEnv injecte dans Maven :
                 * - l'URL SonarQube ;
                 * - le token SonarQube ;
                 * - les paramètres nécessaires à l'analyse.
                 *
                 * 'SonarQube' doit correspondre au nom configuré
                 * dans Manage Jenkins → System → SonarQube servers.
                 */
                withSonarQubeEnv("${SONARQUBE_SERVER}") {
                    dir('backend') {
                        /*
                         * Les tests ont déjà été exécutés dans UNIT TESTS.
                         *
                         * -DskipTests évite de les exécuter une deuxième fois.
                         *
                         * Le rapport JaCoCo généré par Maven est lu
                         * par SonarQube grâce à la propriété du pom.xml :
                         *
                         * sonar.coverage.jacoco.xmlReportPaths
                         */
                        sh 'mvn sonar:sonar'
                    }
                }

                /*
                 * Attend le résultat de la Quality Gate SonarQube.
                 *
                 * SonarQube doit envoyer un webhook à Jenkins :
                 *
                 * http://jenkins:8080/sonarqube-webhook/
                 *
                 * Si la Quality Gate est rouge, le pipeline s'arrête.
                 */
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        /*
         * Frontend CI stages run after both backend checks and before any
         * release artifact is published. This keeps the existing backend
         * deployment flow unchanged while requiring the frontend checks to
         * pass before the monorepo pipeline proceeds.
         */
        stage('FRONTEND INSTALL') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            tools {
                nodejs 'NodeJS24'
            }
            steps {
                dir('frontend') {
                    sh '''
                        node --version
                        npm --version
                        npm ci
                    '''
                }
            }
        }

        stage('FRONTEND UNIT TESTS') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            tools {
                nodejs 'NodeJS24'
            }
            steps {
                dir('frontend') {
                    sh 'npm run test:ci'
                }
            }
            post {
                always {
                    archiveArtifacts(
                        artifacts: 'frontend/coverage/boreal/lcov.info',
                        allowEmptyArchive: true,
                        fingerprint: false
                    )
                }
            }
        }

        stage('FRONTEND BUILD') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            tools {
                nodejs 'NodeJS24'
            }
            steps {
                dir('frontend') {
                    sh '''
                        npm run build
                        test -d dist/boreal/browser
                    '''
                }
            }
        }

        stage('FRONTEND SONARQUBE') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            tools {
                // SonarQube Community Build 26.9 requires Java 21 for analysis.
                jdk 'JDK21'
            }
            steps {
                script {
                    def scannerHome = tool 'SonarScannerCLI'
                    withSonarQubeEnv("${SONARQUBE_SERVER}") {
                        withEnv(["SONAR_SCANNER_HOME=${scannerHome}"]) {
                            dir('frontend') {
                                sh '''
                                    set +x
                                    test -f coverage/boreal/lcov.info
                                    export SONAR_TOKEN="$SONAR_AUTH_TOKEN"
                                    "$SONAR_SCANNER_HOME/bin/sonar-scanner" \\
                                        -Dsonar.scanner.skipJreProvisioning=true
                                '''
                            }
                        }
                    }
                }
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('FRONTEND PUBLISH TO NEXUS') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            tools {
                nodejs 'NodeJS24'
            }
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-admin-frontend-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    dir('frontend') {
                        sh '''
                            set +x
                            set -eu

                            test -d dist/boreal/browser
                            frontend_version="$(node -p "require('./package.json').version")"
                            artifact_name="gestion-station-skii-frontend-${frontend_version}-${BUILD_NUMBER}.zip"
                            artifact_directory="$(mktemp -d)"
                            trap 'rm -rf "$artifact_directory"' EXIT
                            artifact_file="$artifact_directory/$artifact_name"
                            download_file="$artifact_directory/download.zip"

                            jar --create --file "$artifact_file" --no-manifest \\
                                -C dist/boreal/browser .

                            artifact_url="${NEXUS_URL}/repository/${FRONTEND_NEXUS_REPOSITORY}/gestion-station-skii-frontend/${frontend_version}/${BUILD_NUMBER}/${artifact_name}"

                            curl --fail --show-error --silent \\
                                --user "$NEXUS_USERNAME:$NEXUS_PASSWORD" \\
                                --upload-file "$artifact_file" \\
                                "$artifact_url"

                            curl --fail --show-error --silent \\
                                --user "$NEXUS_USERNAME:$NEXUS_PASSWORD" \\
                                --output "$download_file" \\
                                "$artifact_url"

                            cmp --silent "$artifact_file" "$download_file"
                            echo "Frontend artifact published and verified: $artifact_url"
                        '''
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 6 : PUBLISH TO NEXUS
         * ============================================================
         *
         * Publie le fichier JAR dans Nexus.
         *
         * Comme la version du projet est :
         *
         * 0.0.1-SNAPSHOT
         *
         * Maven publiera l'artefact dans maven-snapshots.
         */
        stage('PUBLISH TO NEXUS') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                /*
                 * Récupère temporairement les credentials Nexus
                 * enregistrés dans Jenkins sous :
                 *
                 * nexus-credentials
                 */
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    /*
                     * Récupère temporairement le fichier Maven
                     * enregistré dans Jenkins sous :
                     *
                     * maven-settings
                     */
                    configFileProvider([
                        configFile(
                            fileId: 'maven-settings',
                            variable: 'MAVEN_SETTINGS'
                        )
                    ]) {
                        dir('backend') {
                            /*
                             * deploy :
                             * - construit le package ;
                             * - publie le JAR dans Nexus.
                             *
                             * -s indique à Maven d'utiliser le settings.xml
                             * fourni temporairement par Jenkins.
                             *
                             * -Dnexus.url transmet l'URL de Nexus
                             * au pom.xml.
                             */
                            sh '''
                                mvn deploy \
                                    -DskipTests \
                                    -s "$MAVEN_SETTINGS" \
                                    -Dnexus.url="$NEXUS_URL"
                            '''
                        }
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 7 : BUILDING OUR IMAGE
         * ============================================================
         *
         * Construit l'image Docker depuis backend/Dockerfile.
         *
         * Le Dockerfile copie le JAR généré dans :
         *
         * backend/target/
         */
        stage('BUILDING OUR IMAGE') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                dir('backend') {
                    script {
                        /*
                         * Construit l'image avec un tag versionné.
                         *
                         * Exemple :
                         * fidagh/gestion-station-skii-backend:15
                         */
                        def builtImage = docker.build(
                            "${DOCKER_IMAGE}:${IMAGE_TAG}",
                            "."
                        )

                        /*
                         * Ajoute également le tag latest
                         * à la même image locale.
                         */
                        builtImage.tag('latest')
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 8 : DEPLOY OUR IMAGE
         * ============================================================
         *
         * Se connecte à Docker Hub et pousse les deux tags :
         *
         * - BUILD_NUMBER
         * - latest
         *
         * Le credential Jenkins utilisé est :
         *
         * dockerhub-credentials
         */
        stage('DEPLOY OUR IMAGE') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                script {
                    /*
                     * docker.withRegistry gère automatiquement
                     * la connexion à Docker Hub avec le credential Jenkins.
                     *
                     * Le credential doit être de type :
                     * Username with password
                     *
                     * Le champ Password doit contenir
                     * un Access Token Docker Hub.
                     */
                    docker.withRegistry(
                        'https://index.docker.io/v1/',
                        'dockerhub-credentials'
                    ) {
                        /*
                         * Récupère la référence vers l'image
                         * construite dans BUILDING OUR IMAGE.
                         */
                        def versionedImage = docker.image(
                            "${DOCKER_IMAGE}:${IMAGE_TAG}"
                        )

                        def latestImage = docker.image(
                            "${DOCKER_IMAGE}:latest"
                        )

                        /*
                         * Envoie l'image versionnée sur Docker Hub.
                         */
                        versionedImage.push()

                        /*
                         * Envoie également le tag latest.
                         */
                        latestImage.push()
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 9 : VERIFY DOCKER HUB IMAGE
         * ============================================================
         *
         * Vérifie que l'image peut réellement être récupérée
         * depuis Docker Hub.
         */
        stage('VERIFY DOCKER HUB IMAGE') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                script {
                    /*
                     * Utilise le même credential pour accéder
                     * à Docker Hub pendant le pull.
                     */
                    docker.withRegistry(
                        'https://index.docker.io/v1/',
                        'dockerhub-credentials'
                    ) {
                        sh """
                            # Supprime le tag local versionné.
                            # Le tag latest reste local mais n'est pas utilisé.
                            docker image rm \
                                ${DOCKER_IMAGE}:${IMAGE_TAG} || true

                            # Récupère l'image depuis Docker Hub.
                            docker pull \
                                ${DOCKER_IMAGE}:${IMAGE_TAG}

                            # Vérifie les métadonnées de l'image récupérée.
                            docker image inspect \
                                ${DOCKER_IMAGE}:${IMAGE_TAG}
                        """
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 10 : TEST AWS CREDENTIALS
         * ============================================================
         *
         * Vérifie l'identité AWS du Learner Lab sans créer
         * ni modifier de ressource AWS.
         *
         * Le credential Jenkins doit être de type Secret file
         * et avoir l'identifiant : aws-credentials
         */
        stage('TEST AWS CREDENTIALS') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                withCredentials([
                    file(
                        credentialsId: 'aws-credentials',
                        variable: 'AWS_CREDENTIALS_FILE'
                    )
                ]) {
                    sh '''
                        set +x
                        export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                        export AWS_DEFAULT_REGION="us-east-1"

                        aws --version
                        aws sts get-caller-identity
                    '''
                }
            }
        }

        /*
         * ============================================================
         * Stage 11 : DESTROY AWS INFRASTRUCTURE ONLY (OPTIONAL)
         * ============================================================
         *
         * Ce stage n'est exécuté que si le paramètre de suppression seule
         * est coché. Terraform affiche le plan complet puis attend une
         * approbation avant de supprimer les ressources. Les stages de
         * création et de déploiement sont alors ignorés.
         */
        stage('DESTROY AWS INFRASTRUCTURE ONLY') {
            when {
                expression { params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                dir("${TERRAFORM_DIR}") {
                    sh 'terraform init -input=false'

                    withCredentials([
                        file(
                            credentialsId: 'aws-credentials',
                            variable: 'AWS_CREDENTIALS_FILE'
                        )
                    ]) {
                        sh '''
                            set +x
                            export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                            export AWS_DEFAULT_REGION="$AWS_REGION"
                            export TF_IN_AUTOMATION="true"
                            aws sts get-caller-identity
                            terraform plan -destroy -input=false -out=destroy.tfplan
                        '''
                    }

                    timeout(time: 15, unit: 'MINUTES') {
                        input(
                            message: 'Le plan ci-dessus supprime toutes les ressources Terraform AWS, dont EKS, le réseau et RDS sans snapshot final. Confirmer la suppression uniquement ?',
                            ok: 'Confirmer la suppression complète'
                        )
                    }

                    withCredentials([
                        file(
                            credentialsId: 'aws-credentials',
                            variable: 'AWS_CREDENTIALS_FILE'
                        )
                    ]) {
                        sh '''
                            set +x
                            export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                            export AWS_DEFAULT_REGION="$AWS_REGION"
                            export TF_IN_AUTOMATION="true"
                            terraform apply -input=false destroy.tfplan
                        '''
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 12 : SETUP TERRAFORM
         * ============================================================
         *
         * Regroupe le cycle Terraform comme dans le pipeline de
         * référence : format, init, validate, plan et apply.
         *
         * Une approbation humaine est conservée entre plan et apply.
         */
        stage('SETUP TERRAFORM') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                dir("${TERRAFORM_DIR}") {
                    sh 'terraform fmt -check -recursive'
                    sh 'terraform init -input=false'
                    sh 'terraform validate'

                    withCredentials([
                        file(
                            credentialsId: 'aws-credentials',
                            variable: 'AWS_CREDENTIALS_FILE'
                        )
                    ]) {
                        sh '''
                            set +x
                            export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                            export AWS_DEFAULT_REGION="$AWS_REGION"
                            export TF_IN_AUTOMATION="true"
                            terraform plan -input=false -out=tfplan
                        '''
                    }

                    timeout(time: 15, unit: 'MINUTES') {
                        input(
                            message: 'Le plan Terraform a-t-il été vérifié et peut-il être appliqué sur AWS ?',
                            ok: 'Appliquer le plan'
                        )
                    }

                    withCredentials([
                        file(
                            credentialsId: 'aws-credentials',
                            variable: 'AWS_CREDENTIALS_FILE'
                        )
                    ]) {
                        sh '''
                            set +x
                            export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                            export AWS_DEFAULT_REGION="$AWS_REGION"
                            export TF_IN_AUTOMATION="true"
                            terraform apply -input=false tfplan
                        '''
                    }
                }
            }
        }

        /*
         * ============================================================
         * Stage 13 : UPDATE KUBECONFIG
         * ============================================================
         *
         * Configure kubectl pour le cluster créé par Terraform.
         */
        stage('UPDATE KUBECONFIG') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                withCredentials([
                    file(
                        credentialsId: 'aws-credentials',
                        variable: 'AWS_CREDENTIALS_FILE'
                    )
                ]) {
                    dir("${TERRAFORM_DIR}") {
                        sh '''
                            set +x
                            export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                            export AWS_DEFAULT_REGION="$AWS_REGION"
                            export KUBECONFIG="$WORKSPACE/.kube/config"
                            mkdir -p "$WORKSPACE/.kube"
                            cluster_name="$(terraform output -raw eks_cluster_name)"
                            aws eks update-kubeconfig \
                                --region "$AWS_REGION" \
                                --name "$cluster_name" \
                                --kubeconfig "$KUBECONFIG"
                            kubectl get nodes
                        '''
                    }
                }
            }
        }



                stage('DEPLOY TO AWS KUBERNETES') {
            when {
                expression { !params.DESTROY_AWS_INFRASTRUCTURE_ONLY }
            }
            steps {
                withCredentials([
                    file(
                        credentialsId: 'aws-credentials',
                        variable: 'AWS_CREDENTIALS_FILE'
                    )
                ]) {
                    sh '''
                        set +x
                        export AWS_SHARED_CREDENTIALS_FILE="$AWS_CREDENTIALS_FILE"
                        export AWS_DEFAULT_REGION="$AWS_REGION"
                        export KUBECONFIG="$WORKSPACE/.kube/config"

                        # 1. Adresse de la base et ARN du secret (outputs Terraform)
                        cd "$WORKSPACE/$TERRAFORM_DIR"
                        RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
                        RDS_SECRET_ARN=$(terraform output -raw rds_master_secret_arn)
                        cd "$WORKSPACE"

                        # 2. Identifiants de la base depuis Secrets Manager
                        SECRET_JSON=$(aws secretsmanager get-secret-value \
                            --secret-id "$RDS_SECRET_ARN" \
                            --query SecretString --output text)
                        DB_USER=$(echo "$SECRET_JSON" | jq -r .username)
                        DB_PASS=$(echo "$SECRET_JSON" | jq -r .password)

                        # 3. Namespace
                        kubectl apply -f k8s/namespace.yaml

                        # 4. Secret Kubernetes : DB_URL, DB_USERNAME, DB_PASSWORD
                        kubectl -n stationski create secret generic db-credentials \
                            --from-literal=DB_URL="jdbc:mysql://${RDS_ENDPOINT}:3306/skidb?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8" \
                            --from-literal=DB_USERNAME="$DB_USER" \
                            --from-literal=DB_PASSWORD="$DB_PASS" \
                            --dry-run=client -o yaml | kubectl apply -f -

                        # 5. Deployment (remplace le tag de l'image)
                        sed -e "s|IMAGE_TAG|${IMAGE_TAG}|" \
                            k8s/backend/deployment.yaml | kubectl apply -f -

                        # 6. Service
                        kubectl apply -f k8s/backend/service.yaml

                        # 7. Attendre que les pods soient prêts (sinon afficher les logs)
                        kubectl -n stationski rollout status deployment/backend --timeout=420s || {
                            kubectl -n stationski get pods
                            kubectl -n stationski describe pods -l app=backend | tail -40
                            kubectl -n stationski logs deploy/backend --tail=100
                            exit 1
                        }

                        # 8. Test de santé : directement dans un pod du backend
                        kubectl -n stationski exec deploy/backend -- \
                            wget -qO- http://localhost:8087/stationSki/actuator/health

                        # 8b. Test via le Service (traverse le réseau entre nœuds)
                        kubectl -n stationski delete pod curl-test --ignore-not-found
                        kubectl -n stationski run curl-test --rm -i --restart=Never \
                            --image=curlimages/curl --command -- \
                            curl -sf --max-time 20 http://backend:8087/stationSki/actuator/health

                        # 9. Preuve que Flyway a bien migré
                        kubectl -n stationski logs deploy/backend | grep -i -E "flyway|migrat" || true

                        kubectl -n stationski get pods
                    '''
                }
            }
        }

    }

    /*
     * Actions exécutées après la fin du pipeline.
     */
    post {

        /*
         * Exécuté quel que soit le résultat du pipeline.
         */
        always {
            /*
             * Archive le JAR produit par Maven.
             */
            archiveArtifacts(
                artifacts: 'backend/target/*.jar',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }

        /*
         * Exécuté uniquement si tous les stages réussissent.
         */
        success {
            script {
                if (params.DESTROY_AWS_INFRASTRUCTURE_ONLY) {
                    echo 'Destruction complète de l’infrastructure Terraform terminée.'
                } else {
                    echo 'Pipeline terminé avec succès.'
                    echo "Image publiée : ${DOCKER_IMAGE}:${IMAGE_TAG}"
                }
            }
        }

        /*
         * Exécuté si au moins un stage échoue.
         */
        failure {
            echo 'Pipeline échoué. Vérifiez le stage en erreur.'
        }

        /*
         * Nettoyage des images Docker inutilisées sur l'agent Jenkins.
         */
        cleanup {
            sh '''
                docker image prune -f || true
            '''
        }
    }
}
