#!/usr/bin/env sh
# Validate the information models exactly as CI does, using containers only.
#   ops/scripts/validate-models.sh
set -eu
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

docker build -q -t dtdl-validator docker/dtdl-validator >/dev/null
docker build -q -t aas-validator docker/aas-validator >/dev/null

echo "== DTDL v3"
docker run --rm -v "$ROOT/models/dtdl:/w:ro" dtdl-validator /w

echo "== AAS (Solar.json must match its generator)"
docker run --rm -v "$ROOT/models/aas:/w:ro" --entrypoint python3 aas-validator \
  -I /w/generate_solar_aas.py --check
docker run --rm -v "$ROOT/models/aas:/w:ro" aas-validator /w/Solar.json
