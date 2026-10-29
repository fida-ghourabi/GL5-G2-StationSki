# Jenkins baseline pipeline (Phase 11)

Prepared: root Jenkinsfile, Maven3 tool, Java 17 build runtime in the custom Jenkins image (controller remains Java 21), H2 tests through `crud-test`, Sonar quality gate wait, authenticated Nexus publish and archived test/coverage/JAR results. Docker build/push stages are now included; cloud deployment remains a later phase.

Required credentials: Secret text `sonarqube-token`; username/password `nexus-credentials`. No real credentials belong in Git.

## Before running

- Docker Desktop currently has only 4.2 GiB RAM. Increase its allocation (for example 8 GiB if host memory permits), or use a larger environment before starting Jenkins + Nexus + SonarQube and Maven concurrently. Do not assume all tools will fit.
- In the project root: `docker compose -f compose.devops.yml up -d --build jenkins nexus sonarqube`. Wait for all interfaces to be ready.
- Jenkins runs lab builds on its built-in node. A separate Java 17 agent is preferred for a production setup. The custom image must be rebuilt for base-image security updates.
- Commit and push the Jenkinsfile, Docker/config files, `backend/pom.xml` and `backend/nexus-settings.xml` on your feature branch. Review `git diff` and never add `.env`. No push has been performed by the assistant.

## Create job

1. New Item → `stationski-ci` → Pipeline.
2. Pipeline definition: Pipeline script from SCM.
3. SCM: Git. Repository: `https://github.com/fida-ghourabi/GL5-G2-StationSki.git`.
4. Add Git credentials only if the repository requires authentication.
5. Branch specifier: `*/feat/skip-piste-tests` (verify this is still your intended branch).
6. Script path: `Jenkinsfile`. Save → Build Now.
7. Inspect Console Output and stages. A passing run must include tests, successful Sonar processing/quality gate, and Nexus upload. Capture evidence only after a successful run.

The addresses `http://sonarqube:9000` and `http://nexus:8081` work inside the Compose network, unlike host `localhost` URLs. The Maven Nexus URL remains localhost by default for manual host runs and is overridden only in Jenkins.

Validation performed: Java 17 custom image built and installed; Maven `clean verify -Dspring.profiles.active=crud-test` passed locally with all 26 tests using H2. The baseline Jenkins run succeeded. The new Docker stages still require a full Jenkins run.

## Enable Docker Hub push

1. In Docker Hub, create a personal access token with read/write permission for your repository. Never paste it into Git or chat.
2. Jenkins → Manage Jenkins → Credentials → System → Global → Add Credentials: **Username with password**, username `yassine1234`, password = Docker Hub token, ID **`dockerhub`**.
3. The Docker CLI is installed in the custom Jenkins image. Compose mounts the host socket with supplementary GID `0`, verified for this Docker Desktop instance. This grants host-wide Docker control; use only trusted jobs, not untrusted PRs. A separate agent/daemon is preferred outside this local lab.
4. Commit/push the updated Jenkinsfile, Compose file, Jenkins Dockerfile, backend Dockerfile and `.dockerignore`, and documentation to the configured feature branch. No push is performed automatically.
5. The Jenkins container was already rebuilt/recreated locally. Elsewhere run `docker compose -f compose.devops.yml up -d --build jenkins`.
6. Build Now. Verify **BUILDING OUR IMAGE** and **PUSH TO DOCKER HUB** succeed. The pushed tag is `yassine1234/stationski-backend:jenkins-BUILD_NUMBER` (e.g. `jenkins-2`). No container deployment occurs in this stage.

Login uses `--password-stdin` and a temporary Docker config directory removed afterward. The Docker Hub token is injected only in the push stage. Neither desktop credentials nor `manual-test` are overwritten. Local image cleanup is not automatic; avoid broad Docker prune commands because other project images/volumes share the daemon.

Verified: Docker client/server communication and an image smoke build from inside Jenkins succeeded. Automated authenticated push is pending the credential and job run.
