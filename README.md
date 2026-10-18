# Run StationSki

## Prerequisites

MySQL with database `skidb`, Maven, Java 17 at `~/.jdks/temurin-17`, Node.js 24.x and npm 11+.

## 1. Configure (first time only)

Your local `backend/.env` is already configured and excluded from Git.
On a fresh checkout, copy `backend/.env.example` to `backend/.env` and enter your MySQL password.
Never commit `.env`.

## 2. Start everything

From the project folder:

```bash
./start.sh
```

Starts MySQL if needed (may prompt for sudo), installs frontend dependencies if missing, and launches backend + Angular in the background. First startup can take several minutes.

Open **http://localhost:4200**.

- API docs: http://localhost:8087/stationSki/swagger-ui/index.html
- Logs: `.run/backend.log` and `.run/frontend.log`
- The backend's `local` profile creates/updates database tables.

## 3. Stop the project

```bash
./stop.sh
```

Stops only backend and frontend launched by `start.sh`. MySQL stays running.
To stop database services too:

```bash
sudo systemctl stop httpd php-fpm mysqld
```

## Optional: phpMyAdmin

```bash
db-start
```

Open http://localhost/phpMyAdmin/ and log in using your MySQL credentials.

## DevOps tools (Phase 7)

Requires Docker running and Docker Compose. Run these commands from `GL5-G2-StationSki` (not `backend/`). This stack is separate from `start.sh` / `stop.sh`.

Start Jenkins, SonarQube (with PostgreSQL), Nexus, Prometheus and Grafana:

```bash
docker compose -f compose.devops.yml up -d
```

First startup downloads large images and can take several minutes. Check status and logs:

```bash
docker compose -f compose.devops.yml ps
docker compose -f compose.devops.yml logs --tail=50
```

| Tool | URL | First login |
| --- | --- | --- |
| Jenkins | http://localhost:8080 | Retrieve unlock password below |
| SonarQube | http://localhost:9000 | `admin` / `admin`, then change password |
| Nexus | http://localhost:8081 | `admin`, retrieve password below |
| Prometheus | http://localhost:9090 | No login |
| Grafana | http://localhost:3000 | `admin` / `admin`, then change password |

Retrieve initial passwords after the tools finish initializing:

```bash
docker compose -f compose.devops.yml exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
docker compose -f compose.devops.yml exec nexus cat /nexus-data/admin.password
```

Stop the tools and remove containers, keeping their data:

```bash
docker compose -f compose.devops.yml down
```

Run `up -d` again when needed. **Do not add `-v` to `down`: it deletes the stored data.** Services do not automatically start at boot.

Notes:
- All published ports are restricted to localhost. The database password in Compose is a local-lab placeholder, not a production credential.
- The full stack needs several GiB of RAM. If memory is tight, start a subset, for example `docker compose -f compose.devops.yml up -d sonarqube` (also starts its database).
- SonarQube may require a higher `vm.max_map_count` in the Docker engine host/VM. This host currently has `1048576`; inspect SonarQube logs if it fails to start.
- Prometheus initially scrapes only itself. In Grafana, use `http://prometheus:9090` as the Prometheus data-source URL. Application monitoring comes later.
- Jenkins is pinned to a verified image digest running Jenkins 2.580.1 / Java 21. The application still uses Java 17 separately. Jenkins is not yet configured with Maven/Docker agents or a pipeline. No host Docker socket is mounted.
- Before the Jenkins upgrade, its stopped data volume was archived privately under `~/.local/state/stationski-backups/` (outside Git). Backups contain credentials: keep them private. After any upgrade, review **Manage Jenkins → Plugins → Updates** and security advisories; upgrading core does not automatically update installed plugins.
- Prometheus configuration is copied into a small local image to avoid external-drive bind-mount problems with Docker Desktop. After editing `devops/prometheus.yml`, run `docker compose -f compose.devops.yml up -d --build prometheus`.
- Image tags are moving tags; pin tested versions before making the final CI environment reproducible. All six containers have been observed running; Prometheus readiness was verified. Other tools still require their initial setup.

## Publish backend to Nexus (Phase 9)

1. Start Nexus with the Compose command above and open http://localhost:8081 (startup can take several minutes).
2. Retrieve its initial admin password with the command above. Sign in as `admin`, complete the wizard, and choose a new password. Keep credentials private.
3. Under repository administration, verify a Maven hosted repository named `maven-snapshots` exists with Snapshot version policy. The default installation normally provides it; create it if absent. Use `maven-releases` for future non-SNAPSHOT versions.
4. From `backend/`, run in zsh:

```zsh
set -a
source .env
set +a
export PATH="$JAVA_HOME/bin:$PATH"
export NEXUS_USERNAME=admin
read -s 'NEXUS_PASSWORD?Nexus password: '; echo
export NEXUS_PASSWORD
mvn -s nexus-settings.xml clean deploy
unset NEXUS_PASSWORD
```

MySQL must be running for the existing context test. This compiles, tests, packages, installs locally and uploads the JAR/POM to Nexus. The project version `0.0.1-SNAPSHOT` routes to `maven-snapshots` automatically. The committed settings template reads credentials from the environment; it contains no actual password. For later CI, use a dedicated upload account instead of admin and update URLs when Nexus is not on localhost.

5. In Nexus **Browse → maven-snapshots**, find `com/example/gestion-station-skii/0.0.1-SNAPSHOT`. Verify the timestamped JAR and POM are downloadable and take a screenshot. Only then is Phase 9 complete.

## Jenkins pipeline (Phase 11)

See [Jenkins setup](devops/jenkins-setup.md) for memory prerequisites, required credentials and the feature-branch Pipeline job configuration. The root `Jenkinsfile` builds/tests with Java 17, analyzes with SonarQube and publishes to Nexus. Full pipeline execution is not yet verified.

## Troubleshooting

- Port 8087/4200 occupied: stop the existing backend/frontend first. `stop.sh` does not manage instances started manually.
- Startup failure: check the log files in `.run/`.
- Wrong credentials: edit `backend/.env`, then stop/start the project.
