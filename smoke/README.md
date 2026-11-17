# Smoke test (Lesson 1)

Proves the minimal infrastructure works end to end: the broker starts, passes its healthcheck, and
one MQTT message makes a full round trip.

```bash
smoke/run-smoke.sh
```

What it does (the CI `compose` job runs the same steps):

1. `docker compose config` on the base file and the `smoke` profile.
2. `up -d --wait mosquitto`: starts the broker and blocks until the healthcheck is green.
3. `--profile smoke up --exit-code-from smoke-sub`:
   - `smoke-sub` subscribes to `smoke/test`, waits for one message (max 30 s), prints it, exits 0;
   - `smoke-pub` publishes `hello from rooftwin #N` five times, one per second.
4. Checks that the output contains `smoke/test hello from rooftwin`.

Both one-shot containers reuse the `eclipse-mosquitto:2` image, which ships `mosquitto_pub` and
`mosquitto_sub`, so nothing needs to be built.

Manual equivalent, from the host:

```bash
docker compose -f compose/docker-compose.yml exec mosquitto mosquitto_sub -t 'smoke/#' -v &
docker compose -f compose/docker-compose.yml exec mosquitto mosquitto_pub -t smoke/test -m hello
```
