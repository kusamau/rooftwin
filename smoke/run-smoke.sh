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

restore_broker() {
  # --exit-code-from implies --abort-on-container-exit, which also stops the broker.
  # Remove the one-shot containers and bring the broker back for the next lessons.
  "${COMPOSE[@]}" --profile smoke rm -f smoke-sub smoke-pub >/dev/null
  "${COMPOSE[@]}" up -d --wait mosquitto
}

echo "==> Publishing / subscribing on smoke/test"
if ! "${COMPOSE[@]}" --profile smoke up --exit-code-from smoke-sub smoke-sub smoke-pub \
  2>&1 | tee "$LOG"; then
  echo "==> SMOKE TEST FAILED: broker is healthy locally, but the smoke containers" \
    "could not complete a message round trip" >&2
  echo "    Check that Mosquitto has a listener reachable from the Compose network" \
    "(for example: listener 1883 0.0.0.0)." >&2
  "${COMPOSE[@]}" logs --tail 50 mosquitto >&2
  restore_broker
  exit 1
fi

if grep -q "smoke/test hello from rooftwin" "$LOG"; then
  echo "==> SMOKE TEST PASSED"
else
  echo "==> SMOKE TEST FAILED: no message received" >&2
  "${COMPOSE[@]}" logs --tail 50 mosquitto >&2
  restore_broker
  exit 1
fi

restore_broker
