pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    environment {
        GIT_URL            = 'https://github.com/fida-ghourabi/GL5-G2-StationSki.git'
        GIT_BRANCH_NAME    = 'features/sahar-saidani'

        registry           = 'saharsaidani/gestion-station-skii'
        registryCredential = 'dockerhub'
        dockerImage        = ''

        SONAR_URL          = 'http://sonarqube:9000'
        // Version du projet = 0.0.1-SNAPSHOT => dépôt SNAPSHOT de Nexus (pas "releases")
        NEXUS_SNAPSHOTS    = 'http://nexus:8081/repository/maven-snapshots/'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('CHECKOUT GIT') {
            steps {
                deleteDir()
                git branch: "${GIT_BRANCH_NAME}", url: "${GIT_URL}"
            }
        }

        stage('MVN CLEAN') {
            steps {
                sh 'mvn -B clean'
            }
        }

        stage('ARTIFACT CONSTRUCTION') {
            steps {
                echo 'ARTIFACT CONSTRUCTION...'
                sh 'mvn -B package -DskipTests'
            }
        }

        stage('UNIT TESTS') {
            steps {
                echo 'Launching Unit Tests (JUnit + Mockito + JaCoCo)...'
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('MVN SONARQUBE') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
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

                        mvn -B deploy -DskipTests \
                          -s "$WORKSPACE/settings-nexus.xml" \
                          -DaltDeploymentRepository=nexus-snapshots::default::$NEXUS_SNAPSHOTS

                        rm -f "$WORKSPACE/settings-nexus.xml"
                    '''
                }
            }
        }

        stage('BUILDING OUR IMAGE') {
            steps {
                script {
                    dockerImage = docker.build("${registry}:${BUILD_NUMBER}")
                }
            }
        }

        stage('DEPLOY OUR IMAGE') {
            steps {
                script {
                    docker.withRegistry('', registryCredential) {
                        dockerImage.push()
                        dockerImage.push('latest')
                    }
                }
            }
        }

        // ---- Étapes à ajouter plus tard ----
        // stage('TERRAFORM')  { ... }
        // stage('KUBERNETES') { ... }
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