pipeline {
    /*
     * Jenkins utilise un agent capable d'exécuter :
     * - Maven
     * - Java
     * - Docker
     */
    agent any

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

        /*
         * Nom du serveur SonarQube configuré dans Jenkins.
         */
        SONARQUBE_SERVER = 'SonarQube'
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
            echo 'Pipeline terminé avec succès.'
            echo "Image publiée : ${DOCKER_IMAGE}:${IMAGE_TAG}"
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