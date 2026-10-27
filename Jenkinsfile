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

pipeline {

    agent any

    tools {
        maven 'Maven'
    }

    parameters {
        choice(name: 'ACTION',
               choices: ['apply', 'plan', 'destroy'],
               description: 'apply = build + EKS + deploiement | plan = jusqu au terraform plan | destroy = supprime EKS')
    }

    environment {
        ACTION             = "${params.ACTION ?: 'apply'}"

        registry           = 'saharsaidani/gestion-station-skii'
        registryCredential = 'dockerhub'
        dockerImage        = ''

        SONAR_URL          = 'http://sonarqube:9000'
        NEXUS_SNAPSHOTS    = 'http://nexus:8081/repository/maven-snapshots/'
        BACKEND_DIR        = 'backend'
        TERRAFORM_DIR      = 'terraform'
        TF_IN_AUTOMATION   = 'true'
        TF_PLUGIN_CACHE_DIR = '/var/jenkins_home/.terraform.d/plugin-cache'
        // Etat Terraform conserve hors du workspace
        TF_STATE           = '/var/jenkins_home/tfstate/stationski.tfstate'

        AWS_DEFAULT_REGION = 'us-east-1'
        CLUSTER_NAME       = 'stationski'
        K8S_NAMESPACE      = 'station-ski'

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

        stage('MVN CLEAN') {
            when { expression { env.ACTION != 'destroy' } }
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B clean'
                }
            }
        }

        stage('ARTIFACT CONSTRUCTION') {
            when { expression { env.ACTION != 'destroy' } }
            steps {
                dir("${BACKEND_DIR}") {
                    sh 'mvn -B package -DskipTests'
                }
            }
        }

        stage('UNIT TESTS') {
            when { expression { env.ACTION != 'destroy' } }
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
            when { expression { env.ACTION != 'destroy' } }
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
            when { expression { env.ACTION != 'destroy' } }
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
            when { expression { env.ACTION != 'destroy' } }
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
            when { expression { env.ACTION != 'destroy' } }
            steps {
                script {
                    docker.withRegistry('', registryCredential) {
                        retry(3) { dockerImage.push() }
                        retry(3) { dockerImage.push('latest') }
                    }
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

                                sed "s|IMAGE_PLACEHOLDER|$DEPLOY_IMAGE|g" k8s/app.yaml | kubectl apply -f -
                                kubectl -n "$K8S_NAMESPACE" rollout status deployment/station-ski --timeout=600s
                                kubectl -n "$K8S_NAMESPACE" get pods,svc

                                NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="ExternalIP")].address}')
                                echo "Application : http://$NODE_IP:30080/stationSki/"
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