#!/usr/bin/env bash
# Lesson 1 smoke test: broker healthy + one MQTT message round-trip.
# Same steps as the CI "compose" job, so a green run here means a green run there.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE=(docker compose -f "$ROOT/compose/docker-compose.yml")
LOG="$(mktemp)"
trap 'rm -f "$LOG"' EXIT

echo "==> Validating compose files"
"${COMPOSE[@]}" config --quiet
"${COMPOSE[@]}" --profile smoke config --quiet

echo "==> Starting broker and waiting for the healthcheck"
"${COMPOSE[@]}" up -d --wait mosquitto

echo "==> Publishing / subscribing on smoke/test"
"${COMPOSE[@]}" --profile smoke up --exit-code-from smoke-sub smoke-sub smoke-pub | tee "$LOG"

if grep -q "smoke/test hello from rooftwin" "$LOG"; then
  echo "==> SMOKE TEST PASSED"
else
  echo "==> SMOKE TEST FAILED: no message received" >&2
  "${COMPOSE[@]}" logs mosquitto >&2
  exit 1
fi

# --exit-code-from implies --abort-on-container-exit, which also stops the broker.
# Remove the one-shot containers and bring the broker back for the next lessons.
"${COMPOSE[@]}" --profile smoke rm -f smoke-sub smoke-pub >/dev/null
"${COMPOSE[@]}" up -d --wait mosquitto
