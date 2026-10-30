# Phase 15 — Local application monitoring

No AWS resources are used. This monitors the separate local Docker backend/MySQL demo, not the deleted EKS deployment.

## Open now

- Grafana dashboard: http://localhost:3000/d/stationski-local
- Prometheus targets: http://localhost:9090/targets
- Backend Swagger: http://localhost:8088/stationSki/swagger-ui/index.html
- Metrics: http://localhost:8088/stationSki/actuator/prometheus

Use your existing Grafana login; `admin` / `admin` applies only if the initial password was never changed. Dashboard folder: **StationSki**, dashboard: **StationSki - Local Application Monitoring**. The data source/dashboard are provisioned automatically; no manual creation is required.

## Start later (project root)

```bash
# Start metrics storage/dashboard and create their shared local network.
docker compose -f compose.devops.yml up -d --build prometheus grafana

# First build a fresh JAR after source/config changes.
cd backend
export JAVA_HOME="$HOME/.jdks/temurin-17"
export PATH="$JAVA_HOME/bin:$PATH"
mvn package -Dmaven.test.skip=true
cd ..

# The overlay joins the backend to the monitoring network and enables metrics.
docker compose -f compose.app.yml -f compose.monitoring.yml up -d --build
```

The standalone app still runs without this overlay/metrics profile. Prometheus reaches `backend:8087` through `stationski-monitoring`, not AWS or an external-drive bind mount. Host ports remain localhost-only. Only health/info/prometheus are exposed in the opt-in profile; sensitive endpoints such as `/actuator/env` remain hidden. Do not expose these unauthenticated metrics publicly in production.

## Dashboard panels

1. Backend scrape availability (`1` = UP, `0` = scrape failed; not a full business-health assertion).
2. JVM uptime.
3. HTTP requests/second, excluding Actuator scrapes.
4. Mean HTTP response duration (not p95).
5. JVM heap used/max.
6. JVM process CPU.

Rate panels need at least two scrapes and traffic in their 2-minute window. Wait 30–60 seconds, open Swagger and make some read-only requests; idle latency may show no value because there are no requests. Time range defaults to last 15 minutes. Sample traffic:

```bash
for i in {1..30}; do
  curl -fsS http://localhost:8088/stationSki/v3/api-docs -o /dev/null
  sleep 1
done
```

## Evidence and verification

Screenshot Prometheus Targets showing `stationski-backend` **UP**, and the Grafana dashboard with availability=1, JVM memory and request graphs. No credentials should appear in screenshots.

Verified locally: metrics endpoint HTTP 200, Prometheus `up{job="stationski-backend"}=1`, all seven panel query expressions return data, Grafana health is OK and provisioning completed. Two isolated `MonitoringEndpointTest` cases pass (metrics/application tag, environment endpoint hidden). The packaging command above skips tests; the tests were run separately. Existing full suite was not rerun for this configuration-only production change.

## Stop without deleting data

```bash
docker compose -f compose.app.yml -f compose.monitoring.yml down
docker compose -f compose.devops.yml stop prometheus grafana
```

Do not use `down -v` unless intentionally deleting stored monitoring/application data. SonarQube and its database were stopped temporarily to free RAM; restart them before future CI: `docker compose -f compose.devops.yml up -d sonarqube`.
