pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyser le backend sur SonarQube.')
        booleanParam(name: 'PUBLISH_NEXUS', defaultValue: true, description: 'Publier le JAR dans le dépôt Maven Nexus.')
        booleanParam(name: 'PUSH_IMAGES', defaultValue: true, description: 'Publier les images backend et frontend sur Docker Hub.')
        booleanParam(name: 'PLAN_TERRAFORM', defaultValue: false, description: 'Vérifier Terraform et afficher le plan sans appliquer ni déployer.')
        booleanParam(name: 'APPLY_TERRAFORM', defaultValue: false, description: 'Créer ou mettre à jour VPC, EKS et RDS (coûts AWS).')
        booleanParam(name: 'DEPLOY_TO_EKS', defaultValue: false, description: 'Déployer les images sur le cluster EKS.')
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'Région du Learner Lab.')
        string(name: 'EKS_CLUSTER_NAME', defaultValue: 'stationski-dev', description: 'Nom du cluster Terraform/EKS.')
        string(name: 'EKS_API_CIDR', defaultValue: '', description: 'CIDR public restreint pour Jenkins vers l’API EKS, par exemple son IP sortante suivie de /32.')
        string(name: 'DOCKERHUB_NAMESPACE', defaultValue: 'bennasserhajer', description: 'Namespace Docker Hub propriétaire des images.')
        string(name: 'SONAR_URL', defaultValue: 'http://sonarqube:9000', description: 'URL SonarQube joignable depuis Jenkins.')
        string(name: 'NEXUS_MAVEN_URL', defaultValue: 'http://nexus:8081/repository/maven-snapshots/', description: 'URL du dépôt Maven snapshots Nexus.')
    }

    environment {
        DOCKER_CREDENTIALS    = 'dockerhub'
        SONAR_CREDENTIALS     = 'sonar-token'
        NEXUS_CREDENTIALS     = 'nexus-creds'
        AWS_ACCESS_KEY_ID_CREDENTIALS     = 'aws-access-key-id'
        AWS_SECRET_ACCESS_KEY_CREDENTIALS = 'aws-secret-access-key'
        AWS_SESSION_TOKEN_CREDENTIALS     = 'aws-session-token'
        IMAGE_TAG = 'pending'
        BACKEND_IMAGE = "${params.DOCKERHUB_NAMESPACE}/stationski-backend"
        FRONTEND_IMAGE = "${params.DOCKERHUB_NAMESPACE}/stationski-frontend"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                }
            }
        }

        stage('Vérification des outils') {
            steps {
                sh '''
                    set -eu
                    java -version
                    chmod +x backend/mvnw
                    ./backend/mvnw -version
                    command -v docker
                    command -v terraform
                    command -v aws
                    command -v kubectl
                    command -v jq
                    command -v python3
                '''
            }
        }

        stage('Tests backend et couverture') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw && ./mvnw -B clean verify'
                }
            }
        }

        stage('Analyse SonarQube') {
            when { expression { params.RUN_SONAR } }
            steps {
                withCredentials([string(credentialsId: env.SONAR_CREDENTIALS, variable: 'SONAR_TOKEN')]) {
                    dir('backend') {
                        sh '''
                            ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
                              -Dsonar.projectKey=stationski-backend \
                              -Dsonar.host.url="$SONAR_URL" \
                              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Publication du JAR vers Nexus') {
            when { expression { params.PUBLISH_NEXUS } }
            steps {
                withCredentials([usernamePassword(credentialsId: env.NEXUS_CREDENTIALS,
                                                  usernameVariable: 'NEXUS_USER',
                                                  passwordVariable: 'NEXUS_PASSWORD')]) {
                    dir('backend') {
                        sh '''
                            set +x
                            chmod +x mvnw
                            settings_file="$(mktemp)"
                            trap 'rm -f "$settings_file"' EXIT
                            python3 -c 'import os, xml.sax.saxutils; print("<settings><servers><server><id>stationski-nexus</id><username>" + xml.sax.saxutils.escape(os.environ["NEXUS_USER"]) + "</username><password>" + xml.sax.saxutils.escape(os.environ["NEXUS_PASSWORD"]) + "</password></server></servers></settings>")' > "$settings_file"
                            ./mvnw -B -s "$settings_file" org.apache.maven.plugins:maven-deploy-plugin:3.1.3:deploy-file \
                              -DrepositoryId=stationski-nexus \
                              -Durl="$NEXUS_MAVEN_URL" \
                              -Dfile=target/gestion-station-skii-0.0.1-SNAPSHOT.jar \
                              -DpomFile=pom.xml \
                              -DgeneratePom=false
                        '''
                    }
                }
            }
        }

        stage('Construction des images') {
            steps {
                sh '''
                    docker build -t "$BACKEND_IMAGE:$IMAGE_TAG" backend
                    docker build -t "$FRONTEND_IMAGE:$IMAGE_TAG" frontend
                '''
            }
        }

        stage('Publication Docker Hub') {
            when { expression { params.PUSH_IMAGES || params.DEPLOY_TO_EKS } }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS,
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        set +x
                        printf '%s' "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USER" --password-stdin
                        docker push "$BACKEND_IMAGE:$IMAGE_TAG"
                        docker push "$FRONTEND_IMAGE:$IMAGE_TAG"
                    '''
                }
            }
        }

        stage('Terraform : plan et provisionnement') {
            when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS } }
            steps {
                script {
                    if (env.BRANCH_NAME != 'feature/inscription') {
                        error('Les opérations AWS sont autorisées uniquement depuis feature/inscription.')
                    }
                    if (!params.EKS_API_CIDR?.trim()) {
                        error('Renseigne EKS_API_CIDR avec le CIDR public de Jenkins (par exemple x.x.x.x/32) avant Terraform.')
                    }
                }
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "TF_VAR_aws_region=${params.AWS_REGION}",
                             "TF_VAR_cluster_name=${params.EKS_CLUSTER_NAME}",
                             "TF_VAR_cluster_endpoint_cidr=${params.EKS_API_CIDR}"]) {
                        dir('infrastructure/terraform') {
                            sh '''
                                set +x
                                aws sts get-caller-identity
                                terraform init -input=false
                                terraform fmt -check -recursive
                                terraform validate
                                terraform plan -input=false -out=tfplan
                                if [ "$APPLY_TERRAFORM" = "true" ]; then
                                    terraform apply -input=false -auto-approve tfplan
                                fi
                            '''
                        }
                    }
                }
            }
        }

        stage('Déploiement sur EKS') {
            when { expression { params.DEPLOY_TO_EKS } }
            steps {
                script {
                    if (env.BRANCH_NAME != 'feature/inscription') {
                        error('Le déploiement AWS est autorisé uniquement depuis feature/inscription.')
                    }
                }
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
                        dir('infrastructure/terraform') {
                            script {
                                env.RDS_ENDPOINT = sh(returnStdout: true, script: 'terraform output -raw rds_endpoint').trim()
                                env.RDS_SECRET_ARN = sh(returnStdout: true, script: 'terraform output -raw rds_secret_arn').trim()
                            }
                        }
                        sh '''
                            set +x
                            kubeconfig_file="$(mktemp)"
                            secret_file="$(mktemp)"
                            chmod 600 "$kubeconfig_file" "$secret_file"
                            trap 'rm -f "$kubeconfig_file" "$secret_file"' EXIT
                            export KUBECONFIG="$kubeconfig_file"

                            aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"
                            DB_SECRET_JSON="$(aws secretsmanager get-secret-value --secret-id "$RDS_SECRET_ARN" --query SecretString --output text)"
                            printf '%s' "$DB_SECRET_JSON" | jq -er '"username=" + .username, "password=" + .password' > "$secret_file"

                            kubectl apply -f k8s/namespace.yaml
                            kubectl create secret generic stationski-db \
                              --namespace stationski \
                              --from-env-file="$secret_file" \
                              --dry-run=client -o yaml | kubectl apply -f -
                            kubectl create configmap stationski-backend-config \
                              --namespace stationski \
                              --from-literal="DB_URL=jdbc:mysql://$RDS_ENDPOINT:3306/skidb?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8" \
                              --dry-run=client -o yaml | kubectl apply -f -

                            sed "s|IMAGE_PLACEHOLDER|$BACKEND_IMAGE:$IMAGE_TAG|" k8s/backend.yaml | kubectl apply -f -
                            sed "s|IMAGE_PLACEHOLDER|$FRONTEND_IMAGE:$IMAGE_TAG|" k8s/frontend.yaml | kubectl apply -f -

                            kubectl rollout status deployment/stationski-backend --namespace stationski --timeout=600s
                            kubectl rollout status deployment/stationski-frontend --namespace stationski --timeout=300s
                            frontend_host=""
                            for attempt in $(seq 1 60); do
                                frontend_host="$(kubectl get service stationski-frontend --namespace stationski --output=jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
                                if [ -n "$frontend_host" ]; then break; fi
                                sleep 5
                            done
                            test -n "$frontend_host"
                            printf 'Frontend URL: http://%s\\n' "$frontend_host"
                            kubectl get deployments,services,pods --namespace stationski
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'backend/target/site/jacoco/**,backend/target/surefire-reports/*.xml',
                             allowEmptyArchive: true
            sh 'docker logout >/dev/null 2>&1 || true'
        }
        success {
            echo "Build ${env.BUILD_NUMBER} OK — ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} et ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
        }
        failure {
            echo "Build ${env.BUILD_NUMBER} en échec : consulte la Console Output et les rapports archivés."
        }
    }
}
