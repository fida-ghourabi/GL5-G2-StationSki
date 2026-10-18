// Local coursework baseline. Separate build agents are preferred outside this lab.
// Docker image build/deployment stages will be added in later course phases.
pipeline {
    agent any
    tools { maven 'Maven3' }
    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
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
