// ===================== Emails HTML =====================
// Pour changer l'apparence des emails : modifie uniquement buildMailHtml() ci-dessous.
def htmlEscape(String s) {
    return (s ?: '').replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
}

def buildMailHtml(String title, String color, String subtitle, List rows, String buttonUrl, String buttonLabel) {
    String rowsHtml = ''
    for (r in rows) {
        rowsHtml += '<tr>' +
            '<td style="padding:10px 14px;color:#6b7280;font-size:13px;border-bottom:1px solid #e5e7eb;width:38%">' + htmlEscape(String.valueOf(r[0])) + '</td>' +
            '<td style="padding:10px 14px;color:#111827;font-size:13px;font-weight:600;border-bottom:1px solid #e5e7eb">' + htmlEscape(String.valueOf(r[1])) + '</td>' +
            '</tr>'
    }
    return """<!DOCTYPE html>
<html>
<body style="margin:0;padding:24px 0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif">
  <table align="center" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:8px;overflow:hidden">
    <tr>
      <td style="background:${color};padding:24px 28px">
        <div style="color:#ffffff;font-size:22px;font-weight:700">${htmlEscape(title)}</div>
        <div style="color:#ffffff;opacity:0.9;font-size:14px;margin-top:6px">${htmlEscape(subtitle)}</div>
      </td>
    </tr>
    <tr>
      <td style="padding:20px 28px">
        <table width="100%" cellpadding="0" cellspacing="0" style="border:1px solid #e5e7eb;border-radius:6px">
          ${rowsHtml}
        </table>
      </td>
    </tr>
    <tr>
      <td align="center" style="padding:4px 28px 28px">
        <a href="${buttonUrl}" style="display:inline-block;background:${color};color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;padding:12px 24px;border-radius:6px">${htmlEscape(buttonLabel)}</a>
      </td>
    </tr>
    <tr>
      <td style="background:#f9fafb;padding:14px 28px;color:#9ca3af;font-size:12px;text-align:center">
        Message automatique envoyé par Jenkins
      </td>
    </tr>
  </table>
</body>
</html>"""
}

// Envoie l'email sans jamais faire échouer le build si le serveur mail est indisponible.
def sendMail(String subject, String html) {
    try {
        mail to: params.NOTIFY_EMAIL, subject: subject, body: html, mimeType: 'text/html'
    } catch (Exception e) {
        echo "Envoi de l'email impossible : ${e.message}"
    }
}

def commonMailRows() {
    String duration = (currentBuild.durationString ?: '').replace(' and counting', '')
    return [
        ['Projet', env.JOB_NAME],
        ['Build', "#${env.BUILD_NUMBER}"],
        ['Branche', env.GIT_BRANCH ?: params.GIT_REVISION],
        ['Commit', env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : 'n/a'],
        ['Tag des images', env.IMAGE_TAG],
        ['Durée', duration]
    ]
}


// =======================================================

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 120, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyser le backend sur SonarQube.')
        booleanParam(name: 'PUBLISH_NEXUS', defaultValue: true, description: 'Publier le JAR dans le dépôt Maven Nexus.')
        booleanParam(name: 'PUSH_IMAGES', defaultValue: true, description: 'Publier les images backend et frontend sur Docker Hub.')
        booleanParam(name: 'PLAN_TERRAFORM', defaultValue: true, description: 'Vérifier Terraform et afficher le plan sans appliquer ni déployer.')
        booleanParam(name: 'APPLY_TERRAFORM', defaultValue: true, description: 'Créer ou mettre à jour VPC, EKS et RDS (coûts AWS).')
        booleanParam(name: 'DEPLOY_TO_EKS', defaultValue: true, description: 'Déployer les images sur le cluster EKS.')
        booleanParam(name: 'DEPLOY_MONITORING', defaultValue: true, description: 'Installer Prometheus et Grafana (Helm) apres le deploiement sur EKS.')
        string(name: 'GIT_REVISION', defaultValue: 'feature/hajer-iben-nasr', description: 'Branche Git suivie par ArgoCD (les fichiers k8s/app doivent etre pousses sur cette branche).')
        string(name: 'GIT_REPO_URL', defaultValue: 'https://github.com/fida-ghourabi/GL5-G2-StationSki.git', description: 'URL HTTPS du depot GitHub pour ArgoCD (laisser vide = URL du checkout Jenkins).')
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'Région du Learner Lab.')
        string(name: 'EKS_CLUSTER_NAME', defaultValue: 'stationski-cluster', description: 'Nom du cluster Terraform/EKS.')
        string(name: 'EKS_API_CIDR', defaultValue: '', description: 'CIDR public restreint pour Jenkins vers l’API EKS, par exemple son IP sortante suivie de /32.')
        string(name: 'DOCKERHUB_NAMESPACE', defaultValue: 'bennasserhajer', description: 'Namespace Docker Hub propriétaire des images.')
        string(name: 'SONAR_URL', defaultValue: 'http://sonarqube:9000', description: 'URL SonarQube joignable depuis Jenkins.')
        string(name: 'NEXUS_MAVEN_URL', defaultValue: 'http://nexus:8081/repository/maven-snapshots/', description: 'URL du dépôt Maven snapshots Nexus.')
        choice(name: 'NEXUS_TARGET', choices: ['snapshot', 'release', 'both'], description: 'Type de publication Nexus : snapshot, release ou les deux.')
        string(name: 'RELEASE_VERSION', defaultValue: '1.0.0', description: 'Version utilisée pour la release Nexus (sans -SNAPSHOT).')
        string(name: 'NEXUS_RELEASE_URL', defaultValue: 'http://nexus:8081/repository/maven-releases/', description: 'URL du dépôt Maven releases Nexus.')
        string(name: 'NOTIFY_EMAIL', defaultValue: 'hajerbennacer03@gmail.com', description: 'Adresse qui reçoit les notifications email du build.')
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

                    List startRows = commonMailRows()
                    startRows.add(['Tests + Sonar', "${params.RUN_SONAR}"])
                    startRows.add(['Terraform apply', "${params.APPLY_TERRAFORM}"])
                    startRows.add(['Déploiement EKS', "${params.DEPLOY_TO_EKS}"])
                    startRows.add(['Monitoring', "${params.DEPLOY_MONITORING}"])
                    sendMail("🚀 DÉMARRÉ : ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                             buildMailHtml('Pipeline démarré', '#2563eb', 'Le build vient de commencer.',
                                           startRows, env.BUILD_URL, 'Suivre le build'))
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
                              -Dsonar.projectKey=feature-hajer-iben-nasr \
                              -Dsonar.projectName=feature/hajer-iben-nasr \
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
                            JAR="$(ls target/gestion-station-skii-*.jar | grep -v original | head -1)"
                            echo "JAR à publier : $JAR"

                            if [ "$NEXUS_TARGET" = "snapshot" ] || [ "$NEXUS_TARGET" = "both" ]; then
                                echo "=== Publication SNAPSHOT ==="
                                ./mvnw -B -s "$settings_file" org.apache.maven.plugins:maven-deploy-plugin:3.1.3:deploy-file \\
                                  -DrepositoryId=stationski-nexus \\
                                  -Durl="$NEXUS_MAVEN_URL" \\
                                  -Dfile="$JAR" \\
                                  -DpomFile=pom.xml \\
                                  -DgeneratePom=false
                            fi

                            if [ "$NEXUS_TARGET" = "release" ] || [ "$NEXUS_TARGET" = "both" ]; then
                                echo "=== Publication RELEASE $RELEASE_VERSION ==="
                                ./mvnw -B -s "$settings_file" org.apache.maven.plugins:maven-deploy-plugin:3.1.3:deploy-file \\
                                  -DrepositoryId=stationski-nexus \\
                                  -Durl="$NEXUS_RELEASE_URL" \\
                                  -Dfile="$JAR" \\
                                  -DgroupId=com.example \\
                                  -DartifactId=gestion-station-skii \\
                                  -Dversion="$RELEASE_VERSION" \\
                                  -Dpackaging=jar \\
                                  -DgeneratePom=true
                            fi
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

        stage('Vérification connexion AWS') {
            when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS } }
            steps {
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}", "AWS_DEFAULT_REGION=${params.AWS_REGION}"]) {
                        sh '''
                            set +x
                            echo 'Vérification de la connexion AWS et de l’identité utilisée par Jenkins...'
                            aws sts get-caller-identity --output table
                        '''
                    }
                }
            }
        }

        stage('Terraform : plan et provisionnement') {
            when { expression { params.PLAN_TERRAFORM || params.APPLY_TERRAFORM || params.DEPLOY_TO_EKS } }
            steps {
                script {
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
                            chmod 600 "$kubeconfig_file"
                            trap 'rm -f "$kubeconfig_file"' EXIT
                            export KUBECONFIG="$kubeconfig_file"

                            aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"
                            DB_SECRET_JSON="$(aws secretsmanager get-secret-value --secret-id "$RDS_SECRET_ARN" --query SecretString --output text)"

                            # ---- Ansible prépare le cluster : namespace, secret, configmap, ArgoCD, Application ----
                            command -v ansible-playbook >/dev/null 2>&1 || { echo 'ansible-playbook est requis sur l agent Jenkins (apt-get install -y ansible).' >&2; exit 1; }
                            python3 -c 'import kubernetes' 2>/dev/null || { echo 'Le module Python kubernetes est requis (apt-get install -y python3-kubernetes).' >&2; exit 1; }

                            # Les identifiants RDS passent par l'environnement (jamais écrits dans un fichier ni dans les logs)
                            export RDS_USERNAME="$(printf '%s' "$DB_SECRET_JSON" | jq -er .username)"
                            export RDS_PASSWORD="$(printf '%s' "$DB_SECRET_JSON" | jq -er .password)"
                            export RDS_ENDPOINT

                            ARGO_REPO_URL="${GIT_REPO_URL:-$GIT_URL}"
                            test -n "$ARGO_REPO_URL" || { echo "GIT_REPO_URL vide et GIT_URL indisponible" >&2; exit 1; }
                            BACKEND_DEPLOY_IMAGE="$BACKEND_IMAGE:$IMAGE_TAG"
                            FRONTEND_DEPLOY_IMAGE="$FRONTEND_IMAGE:$IMAGE_TAG"

                            (
                                cd ansible
                                ansible-galaxy collection install -r requirements.yml
                                ansible-playbook bootstrap-eks.yml \
                                  -e "argo_repo_url=$ARGO_REPO_URL" \
                                  -e "git_revision=$GIT_REVISION" \
                                  -e "backend_image=$BACKEND_DEPLOY_IMAGE" \
                                  -e "frontend_image=$FRONTEND_DEPLOY_IMAGE"
                            )

                            # Attend qu'ArgoCD soit démarré (kubectl reste utilisé pour les attentes)
                            kubectl -n argocd rollout status deployment/argocd-server --timeout=300s
                            kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=300s
                            kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=300s

                            # Attend qu'ArgoCD ait deploye les bonnes images
                            CUR_BACK=""
                            CUR_FRONT=""
                            for i in $(seq 1 60); do
                                CUR_BACK=$(kubectl -n stationski get deploy stationski-backend \
                                           -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                                CUR_FRONT=$(kubectl -n stationski get deploy stationski-frontend \
                                            -o jsonpath='{.spec.template.spec.containers[0].image}' 2>/dev/null || true)
                                [ "$CUR_BACK" = "$BACKEND_DEPLOY_IMAGE" ] && [ "$CUR_FRONT" = "$FRONTEND_DEPLOY_IMAGE" ] && break
                                sleep 5
                            done
                            if [ "$CUR_BACK" != "$BACKEND_DEPLOY_IMAGE" ] || [ "$CUR_FRONT" != "$FRONTEND_DEPLOY_IMAGE" ]; then
                                echo "ArgoCD n'a pas synchronise les images attendues" >&2
                                kubectl -n argocd get applications
                                kubectl -n argocd describe application station-ski | tail -40
                                exit 1
                            fi

                            kubectl -n argocd get applications
                            ARGO_HOST=""
                            for i in $(seq 1 60); do
                                ARGO_HOST="$(kubectl -n argocd get svc argocd-server -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
                                [ -n "$ARGO_HOST" ] && break
                                sleep 5
                            done
                            echo "ArgoCD : http://$ARGO_HOST (utilisateur admin)"
                            echo "Mot de passe : kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d"

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
                            command -v curl >/dev/null 2>&1 || { echo 'curl est requis pour le smoke test EKS.' >&2; exit 1; }
                            api_url="http://$frontend_host/stationSki/registration/skier/1"

                            # Etape A : attendre que l'adresse du LoadBalancer soit resolvable (jusqu'a 10 min)
                            for attempt in $(seq 1 40); do
                                getent hosts "$frontend_host" >/dev/null 2>&1 && break
                                echo "DNS pas encore pret ($attempt/40)..."
                                sleep 15
                            done

                            # Etape B : tester l'API (jusqu'a 5 min)
                            api_status=""
                            for attempt in $(seq 1 30); do
                                api_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
                                    --connect-timeout 5 --max-time 15 "$api_url" || true)"
                                case "$api_status" in
                                    2??|404) break ;;
                                esac
                                echo "API pas prete (HTTP $api_status), tentative $attempt/30..."
                                sleep 10
                            done
                            case "$api_status" in
                                2??|404) printf 'Smoke test API réussi : HTTP %s\n' "$api_status" ;;
                                *) echo "Smoke test API échoué : HTTP ${api_status:-aucune réponse} sur $api_url" >&2; exit 1 ;;
                            esac
                            kubectl get deployments,services,pods --namespace stationski
                        '''
                    }
                }
            }
        }

        stage('Monitoring Prometheus et Grafana') {
            when { expression { params.DEPLOY_TO_EKS && params.DEPLOY_MONITORING } }
            steps {
                withCredentials([
                    string(credentialsId: env.AWS_ACCESS_KEY_ID_CREDENTIALS, variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: env.AWS_SECRET_ACCESS_KEY_CREDENTIALS, variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: env.AWS_SESSION_TOKEN_CREDENTIALS, variable: 'AWS_SESSION_TOKEN'),
                    string(credentialsId: 'grafana-admin-password', variable: 'GRAFANA_PASS')
                ]) {
                    withEnv(["AWS_REGION=${params.AWS_REGION}",
                             "EKS_CLUSTER_NAME=${params.EKS_CLUSTER_NAME}"]) {
                        sh '''
                            set +x
                            command -v helm >/dev/null 2>&1 || { echo 'helm est requis sur l agent Jenkins.' >&2; exit 1; }
                            kubeconfig_file="$(mktemp)"
                            chmod 600 "$kubeconfig_file"
                            trap 'rm -f "$kubeconfig_file"' EXIT
                            export KUBECONFIG="$kubeconfig_file"
                            aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"

                            helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
                            helm repo update
                            helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
                              --namespace monitoring --create-namespace \
                              -f k8s/monitoring/values.yaml \
                              --set-string grafana.adminPassword="$GRAFANA_PASS" \
                              --wait --timeout 15m

                            kubectl -n monitoring get pods
                            GRAFANA_HOST=""
                            for i in $(seq 1 60); do
                                GRAFANA_HOST="$(kubectl -n monitoring get svc monitoring-grafana -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
                                [ -n "$GRAFANA_HOST" ] && break
                                sleep 5
                            done
                            echo "Grafana : http://$GRAFANA_HOST (utilisateur admin)"
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
            dir('backend') {
                sh './mvnw -B clean'
            }
            sh 'docker logout >/dev/null 2>&1 || true'
        }
        success {
            echo "Build ${env.BUILD_NUMBER} OK — ${env.BACKEND_IMAGE}:${env.IMAGE_TAG} et ${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"
            echo "L'infrastructure AWS reste en place : pense à la détruire manuellement (coûts AWS)."
            script {
                List rows = commonMailRows()
                rows.add(['Image backend', "${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"])
                rows.add(['Image frontend', "${env.FRONTEND_IMAGE}:${env.IMAGE_TAG}"])
                rows.add(['Nexus', "${params.NEXUS_TARGET}"])
                rows.add(['Déployé sur EKS', params.DEPLOY_TO_EKS ? 'Oui' : 'Non'])
                rows.add(['Rappel', "L'infrastructure AWS n'est pas détruite (coûts)"])
                sendMail("✅ SUCCÈS : ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                         buildMailHtml('Pipeline réussi', '#16a34a', 'Toutes les étapes se sont terminées sans erreur.',
                                       rows, env.BUILD_URL, 'Voir le build'))
            }
        }
        failure {
            echo "Build ${env.BUILD_NUMBER} en échec : consulte la Console Output. L'infrastructure AWS n'est PAS détruite."
            script {
                List rows = commonMailRows()
                rows.add(['Rappel', "L'infrastructure AWS n'est PAS détruite"])
                sendMail("❌ ÉCHEC : ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                         buildMailHtml('Pipeline en échec', '#dc2626', 'Une étape a échoué. Consulte les logs pour le détail.',
                                       rows, "${env.BUILD_URL}console", 'Ouvrir les logs'))
            }
        }
        unstable {
            script {
                sendMail("⚠️ INSTABLE : ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                         buildMailHtml('Pipeline instable', '#d97706', 'Le build est terminé mais des tests sont en échec.',
                                       commonMailRows(), env.BUILD_URL, 'Voir les résultats'))
            }
        }
        aborted {
            script {
                sendMail("⏹️ ANNULÉ : ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                         buildMailHtml('Pipeline annulé', '#6b7280', 'Le build a été interrompu.',
                                       commonMailRows(), env.BUILD_URL, 'Voir le build'))
            }
        }
    }
}
