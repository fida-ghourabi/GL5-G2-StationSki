# Jenkins baseline pipeline (Phase 11)

Prepared: root Jenkinsfile, Maven3 tool, Java 17 build runtime in the custom Jenkins image (controller remains Java 21), H2 tests through `crud-test`, Sonar quality gate wait, authenticated Nexus publish and archived test/coverage/JAR results. Docker and cloud deployment are later phases.

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

Validation performed: Java 17 custom image built and installed; Maven `clean verify -Dspring.profiles.active=crud-test` passed locally with all 26 tests using H2. The complete Jenkins pipeline and its credentials have not yet been verified in a job.
