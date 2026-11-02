#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="${APP_NAME:-kien-shop-cart}"
IMAGE="${IMAGE:?IMAGE is required (for example: user/repository:full-commit-sha)}"
: "${DB_URL:?DB_URL is required}" "${DB_USERNAME:?DB_USERNAME is required}" "${DB_PASSWORD:?DB_PASSWORD is required}"
HOST_PORT="${HOST_PORT:-8080}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health/readiness}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-5}"
JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAMPercentage=75.0}"
export DB_URL DB_USERNAME DB_PASSWORD

if [[ ! "$IMAGE" =~ ^[[:alnum:]_.-]+(/[[:alnum:]_.-]+)+:[a-f0-9]{40}$ ]]; then
  printf '%s [deploy] IMAGE must be a Docker Hub repository tagged with a full lowercase commit SHA\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >&2
  exit 2
fi

if [[ ! "$HEALTH_RETRIES" =~ ^[1-9][0-9]*$ || ! "$HEALTH_INTERVAL" =~ ^[1-9][0-9]*$ ]]; then
  printf '%s [deploy] HEALTH_RETRIES and HEALTH_INTERVAL must be positive integers\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" >&2
  exit 2
fi

log() { printf '%s [deploy] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

start_container() {
  docker rm -f "$APP_NAME" >/dev/null 2>&1 || true
  docker run -d \
    --name "$APP_NAME" \
    --restart unless-stopped \
    -p "${HOST_PORT}:8080" \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_URL -e DB_USERNAME -e DB_PASSWORD \
    -e JAVA_OPTS="$JAVA_OPTS" \
    -e TZ=UTC \
    "$1" >/dev/null
}

wait_until_healthy() {
  local attempt
  for ((attempt = 1; attempt <= HEALTH_RETRIES; attempt++)); do
    if [[ "$(docker inspect -f '{{.State.Running}}' "$APP_NAME" 2>/dev/null || echo false)" != "true" ]]; then
      log "container is not running (attempt ${attempt}/${HEALTH_RETRIES})"
      return 1
    fi
    if curl -fsS --max-time 3 -o /dev/null "http://127.0.0.1:${HOST_PORT}${HEALTH_PATH}"; then
      log "healthy after ${attempt} attempt(s)"
      return 0
    fi
    log "not healthy yet (attempt ${attempt}/${HEALTH_RETRIES})"
    sleep "$HEALTH_INTERVAL"
  done
  return 1
}

previous_image=""
if docker inspect "$APP_NAME" >/dev/null 2>&1; then
  previous_image="$(docker inspect -f '{{.Config.Image}}' "$APP_NAME")"
fi
log "previous image: ${previous_image:-<none>}"
log "new image: ${IMAGE}"

docker pull "$IMAGE"

rollback() {
  if [[ -z "$previous_image" ]]; then
    log "no previous image to roll back to"
    docker rm -f "$APP_NAME" >/dev/null 2>&1 || true
    return 1
  fi

  log "rolling back to ${previous_image}"
  if start_container "$previous_image" && wait_until_healthy; then
    log "rollback succeeded; service is running ${previous_image}"
    return 0
  fi

  log "ROLLBACK FAILED; manual intervention required"
  return 2
}

if ! start_container "$IMAGE"; then
  log "new container failed to start"
  docker logs --tail 100 "$APP_NAME" 2>&1 || true
  if rollback; then
    exit 1
  else
    rollback_status=$?
    if [[ "$rollback_status" -eq 2 ]]; then
      exit 2
    fi
    exit 1
  fi
fi

if wait_until_healthy; then
  log "deployment succeeded"
  docker image prune -f >/dev/null 2>&1 || true
  exit 0
fi

log "new image failed its readiness check; last container logs follow"
docker logs --tail 100 "$APP_NAME" 2>&1 || true

if rollback; then
  exit 1
else
  rollback_status=$?
  if [[ "$rollback_status" -eq 2 ]]; then
    exit 2
  fi
  exit 1
fi
