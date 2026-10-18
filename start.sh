#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$PWD"
mkdir -p .run
if [[ ! -f backend/.env ]]; then
  echo 'Create backend/.env from backend/.env.example first.'; exit 1
fi
set -a
source backend/.env
set +a
export PATH="${JAVA_HOME:?Set JAVA_HOME in backend/.env}/bin:$PATH"
command -v mvn >/dev/null
command -v npm >/dev/null
for service in mysqld; do
  if ! systemctl is-active --quiet "$service"; then sudo systemctl start "$service"; fi
done
for port in 8087 4200; do
  if ss -H -ltn "sport = :$port" | grep -q .; then
    echo "Port $port is already in use. Stop the existing app first."; exit 1
  fi
done
for name in backend frontend; do
  if [[ -f .run/$name.pid ]] && kill -0 "$(<".run/$name.pid")" 2>/dev/null; then
    echo "$name is already running. Use ./stop.sh first."; exit 1
  fi
done
if [[ ! -d frontend/node_modules ]]; then (cd frontend && npm ci); fi
cleanup() { "$ROOT/stop.sh"; }
trap cleanup ERR
(
  cd backend
  setsid mvn spring-boot:run -Dspring-boot.run.profiles=local > "$ROOT/.run/backend.log" 2>&1 < /dev/null &
  echo $! > "$ROOT/.run/backend.pid"
)
(
  cd frontend
  setsid npm start > "$ROOT/.run/frontend.log" 2>&1 < /dev/null &
  echo $! > "$ROOT/.run/frontend.pid"
)
echo 'Starting backend and frontend...'
for attempt in {1..120}; do
  for name in backend frontend; do
    if ! kill -0 "$(<".run/$name.pid")" 2>/dev/null; then
      echo "$name failed. See .run/$name.log"; cleanup; exit 1
    fi
  done
  if curl -fsS http://localhost:8087/stationSki/v3/api-docs >/dev/null 2>&1 && curl -fsS http://localhost:4200 >/dev/null 2>&1; then
    echo 'App ready: http://localhost:4200'
    echo 'Logs: .run/backend.log and .run/frontend.log'
    echo 'Stop: ./stop.sh'
    exit 0
  fi
  sleep 2
done
echo 'Still starting. Check .run/backend.log and .run/frontend.log.'
echo 'Stop with ./stop.sh.'
