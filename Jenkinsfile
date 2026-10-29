// Local coursework baseline. Separate build agents are preferred outside this lab.
// Docker socket access is trusted-lab only. Do not build untrusted PRs here.
pipeline {
    agent any
    tools { maven 'Maven3' }
    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
        IMAGE_REPOSITORY = 'yassine1234/stationski-backend'
        JAVA_HOME = '/opt/java17'
        PATH = "/opt/java17/bin:${env.PATH}"
    }
    stages {
        stage('CHECKOUT GIT') {
            steps { checkout scm }
        }
        stage('MVN CLEAN') {
            steps {
                dir('backend') {
                    sh 'java -version && mvn -version && mvn -B clean'
                }
            }
        }
        stage('ARTIFACT CONSTRUCTION') {
            steps {
                dir('backend') { sh 'mvn -B package -Dmaven.test.skip=true' }
            }
        }
        stage('UNIT AND CRUD TESTS') {
            steps {
                dir('backend') { sh 'mvn -B test -Dspring.profiles.active=crud-test' }
            }
        }
        stage('MVN SONARQUBE') {
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    dir('backend') {
                        sh 'mvn -B sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -Dsonar.projectKey=stationski -Dsonar.qualitygate.wait=true'
                    }
                }
            }
        }
        stage('PUBLISH TO NEXUS') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                    dir('backend') {
                        sh 'mvn -B -s nexus-settings.xml deploy -DskipTests -Dnexus.url=http://nexus:8081'
                    }
                }
            }
        }
        stage('BUILDING OUR IMAGE') {
            steps {
                sh 'docker version'
                sh 'docker build -t "$IMAGE_REPOSITORY:jenkins-$BUILD_NUMBER" backend'
            }
        }
        stage('PUSH TO DOCKER HUB') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh '''
                        set +x
                        export DOCKER_CONFIG="$(mktemp -d)"
                        trap 'rm -rf "$DOCKER_CONFIG"' EXIT
                        printf '%s' "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin
                        docker push "$IMAGE_REPOSITORY:jenkins-$BUILD_NUMBER"
                    '''
                }
            }
        }
    }
    post {
        always {
            junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'backend/target/site/jacoco/**', allowEmptyArchive: true
        }
        success {
            archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
        }
    }
}
