# Jenkins baseline pipeline (Phase 11)

Prepared: root Jenkinsfile, Maven3 tool, Java 17 build runtime in the custom Jenkins image (controller remains Java 21), H2 tests through `crud-test`, Sonar quality gate wait, authenticated Nexus publish and archived test/coverage/JAR results. Docker build/push stages are now included; cloud deployment remains a later phase.

Required credentials: Secret text `sonarqube-token`; username/password `nexus-credentials`. No real credentials belong in Git.

## Team integration warning

This personal branch includes the latest team `develop` tests. The earlier 26-test CI success predates that integration. Jenkins now uses the `test` profile with `TEST_DB_*` pointing exclusively to the disposable `ci-db` service (no published port, tmpfs data), while each class retains its own profile and Testcontainers configuration. Do not point these settings at application data: the tests create/drop tables. `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` allows tests inside Jenkins to reach Docker Desktop's mapped container ports. The team JaCoCo configuration runs at `verify`, including its registration coverage gate; the test stage now invokes `mvn verify`.

Validation ran inside the Jenkins container with the actual Docker socket, Java 17 and Maven3: **203 tests passed, 0 failed, 0 skipped**, and all JaCoCo coverage gates passed (`mvn clean verify`). On this personal branch only, stale repository method stubs in `BusinessRulesTest` and `RegistrationServicesMockTest` were aligned with the service queries, and the planning mock now supplies `existsById`. No assertions were weakened, no tests were disabled, and production business logic was unchanged. Shared `develop` was not modified by these CI fixes. The complete Jenkins job (including Sonar/Nexus/Docker publishing) must still be rerun on the new branch.

Recreate the local environment after pulling configuration changes: `docker compose -f compose.devops.yml up -d jenkins`. This also starts its isolated CI database. Only one job should use that shared CI database at a time; separate jobs require separate databases.

## Memory and Nexus availability

Build #5 passed SonarQube's quality gate but could not resolve `nexus` because Docker had OOM-killed the container (exit 137). Nexus now uses a smaller **local-lab-only** JVM heap (512 MiB initial, 1 GiB maximum, 512 MiB direct memory). This is not production sizing or a guarantee against future memory pressure.

Before CI, stop optional workloads and start the required services from the project root:

```bash
docker compose -f compose.app.yml stop
docker compose -f compose.devops.yml stop grafana prometheus
docker compose -f compose.devops.yml up -d jenkins nexus sonarqube
```

Wait for Nexus at http://localhost:8081 and SonarQube at http://localhost:9000. Keep monitoring memory, since team Testcontainers suites start additional MySQL containers. Configuration recreation keeps the existing Nexus data volume. Nexus startup and hostname resolution from Jenkins were verified after recovery. Rerun the complete job; subsequent Docker stages still need verification.

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
5. Branch specifier: `*/feature/yassine-mathlouthi`.
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
