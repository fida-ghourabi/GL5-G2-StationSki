pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    environment {
        registry           = 'saharsaidani/gestion-station-skii'
        registryCredential = 'dockerhub'
        dockerImage        = ''

        SONAR_URL          = 'http://sonarqube:9000'
        // Version du projet 0.0.x-SNAPSHOT => depot SNAPSHOT de Nexus
        NEXUS_SNAPSHOTS    = 'http://nexus:8081/repository/maven-snapshots/'
        BACKEND_DIR        = 'backend'
        TERRAFORM_DIR      = 'terraform'
        TF_IN_AUTOMATION   = 'true'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('CHECKOUT GIT') {
            steps {
                checkout scm
            }
        }

        stage('MVN CLEAN') {
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B clean'
                }
            }
        }

        stage('ARTIFACT CONSTRUCTION') {
            steps {
                echo 'ARTIFACT CONSTRUCTION...'
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B package -DskipTests'
                }
            }
        }

        stage('UNIT TESTS') {
            steps {
                echo 'Launching Unit Tests (JUnit + Mockito + JaCoCo)...'
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('MVN SONARQUBE') {
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
            steps {
                script {
                    // Version lue dans backend/pom.xml (ex. 0.0.2-SNAPSHOT -> 0.0.2)
                    def pomVersion = sh(
                        script: "cd ${BACKEND_DIR} && mvn -q -B help:evaluate -Dexpression=project.version -DforceStdout",
                        returnStdout: true
                    ).trim()
                    env.IMAGE_TAG = "${pomVersion.replace('-SNAPSHOT', '')}-${BUILD_NUMBER}"
                    echo "Image : ${registry}:${env.IMAGE_TAG}"

                    // Le Dockerfile est dans backend/, qui sert de contexte de build
                    dockerImage = docker.build("${registry}:${env.IMAGE_TAG}", "${BACKEND_DIR}")
                }
            }
        }

        stage('DEPLOY OUR IMAGE') {
            steps {
                script {
                    docker.withRegistry('', registryCredential) {
                        dockerImage.push()          // ex. 0.0.2-1
                        dockerImage.push('latest')
                    }
                }
            }
        }

        stage('TERRAFORM VALIDATE') {
            steps {
                dir("${TERRAFORM_DIR}") {
                    sh 'terraform version'

                    // Format : ne bloque pas le pipeline (stage jaune si probleme)
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        sh 'terraform fmt -check -recursive -diff'
                    }

                    // init et validate ne demandent pas de credentials AWS
                    sh 'terraform init -input=false'
                    sh 'terraform validate'
                }
            }
        }

        // ---- Etapes a ajouter plus tard ----
        // TERRAFORM PLAN/APPLY
        // KUBERNETES
    }

    post {
        success {
            echo 'Pipeline terminé avec succès.'
        }
        failure {
            echo 'Pipeline en échec : consulter les logs.'
        }
        always {
            sh 'rm -f "$WORKSPACE/settings-nexus.xml" || true'
        }
    }
}