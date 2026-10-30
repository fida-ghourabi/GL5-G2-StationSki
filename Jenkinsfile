// Local coursework baseline. Separate build agents are preferred outside this lab.
// Docker socket access is trusted-lab only. Do not build untrusted PRs here.
pipeline {
    agent any
    tools { maven 'Maven3' }
    parameters {
        booleanParam(name: 'AWS_DEMO', defaultValue: false,
            description: 'Creates BILLABLE temporary EKS resources. Requires fresh lab credentials and plan approval. Auto-cleanup follows.')
        string(name: 'AWS_ADMIN_CIDR', defaultValue: '',
            description: 'Current workstation public IPv4 /32 for EKS API access. Required only for AWS_DEMO.')
    }
    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 120, unit: 'MINUTES')
    }
    environment {
        IMAGE_REPOSITORY = 'yassine1234/stationski-backend'
        AWS_EXPECTED_ACCOUNT = '085397751671'
        AWS_DEMO_STATE_DIR = "${env.JENKINS_HOME}/stationski-aws-demo"
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
                dir('backend') { // Team MySQL tests use the dedicated CI DB; other suites start Testcontainers.
                    // Preserve each test class's own @ActiveProfiles declaration.
                    sh 'mvn -B verify' }
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
        stage('AWS PLAN - OPT IN') {
            when { expression { params.AWS_DEMO } }
            steps {
                withCredentials([file(credentialsId: 'aws-university-credentials', variable: 'AWS_SHARED_CREDENTIALS_FILE')]) {
                    withEnv(["AWS_ADMIN_CIDR=${params.AWS_ADMIN_CIDR}"]) {
                        sh 'bash devops/aws-demo.sh plan'
                    }
                }
                timeout(time: 10, unit: 'MINUTES') {
                    input message: 'Review Terraform plan. Approve ~$0.15/hour baseline EKS demo costs until cleanup? No hard spending cap.',
                        ok: 'Approve billable demo', submitter: 'yassine'
                }
            }
        }
        stage('AWS TERRAFORM APPLY') {
            when { expression { params.AWS_DEMO } }
            steps {
                script {
                    // Persistent per-build guard; declarative environment values cannot
                    // reliably be changed by assigning env.X from a script block.
                    writeFile file: "${env.AWS_DEMO_STATE_DIR}/cleanup-owner", text: env.BUILD_TAG
                }
                withCredentials([file(credentialsId: 'aws-university-credentials', variable: 'AWS_SHARED_CREDENTIALS_FILE')]) {
                    withEnv(["AWS_ADMIN_CIDR=${params.AWS_ADMIN_CIDR}"]) {
                        sh 'bash devops/aws-demo.sh apply'
                    }
                }
            }
        }
        stage('DEPLOY TO AWS KUBERNETES') {
            when { expression { params.AWS_DEMO } }
            steps {
                withCredentials([file(credentialsId: 'aws-university-credentials', variable: 'AWS_SHARED_CREDENTIALS_FILE')]) {
                    withEnv(["AWS_ADMIN_CIDR=${params.AWS_ADMIN_CIDR}", "AWS_DEMO_IMAGE=${env.IMAGE_REPOSITORY}:jenkins-${env.BUILD_NUMBER}"]) {
                        sh 'bash devops/aws-demo.sh deploy'
                    }
                }
            }
        }
        stage('SCREENSHOTS - MAX 15 MINUTES') {
            when { expression { params.AWS_DEMO } }
            steps {
                echo 'Take EKS/pods screenshots now. Screenshot access instructions are in devops/aws-pipeline.md. Cleanup begins after confirmation or timeout.'
                timeout(time: 15, unit: 'MINUTES') {
                    input message: 'Demo is BILLABLE. Screenshots saved? Proceed to destroy now.',
                        ok: 'Screenshots saved - DELETE AWS demo', submitter: 'yassine'
                }
            }
        }
    }
    post {
        always {
            script {
                if (params.AWS_DEMO &&
                    fileExists("${env.AWS_DEMO_STATE_DIR}/cleanup-owner") &&
                    readFile("${env.AWS_DEMO_STATE_DIR}/cleanup-owner").trim() == env.BUILD_TAG) {
                    timeout(time: 60, unit: 'MINUTES') {
                        withCredentials([file(credentialsId: 'aws-university-credentials', variable: 'AWS_SHARED_CREDENTIALS_FILE')]) {
                            withEnv(["AWS_ADMIN_CIDR=${params.AWS_ADMIN_CIDR}"]) {
                                sh 'bash devops/aws-demo.sh cleanup'
                            }
                        }
                    }
                }
            }
            junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'backend/target/site/jacoco/**', allowEmptyArchive: true
        }
        success {
            archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
        }
    }
}
