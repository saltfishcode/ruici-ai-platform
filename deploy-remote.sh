#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.yml}"
DEPLOY_MODE="${2:-oneclick}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REMOTE_ENV_TEMPLATE="deploy/.env.ecs.example"
SWAP_FILE="/swapfile"
SWAP_SIZE_MB="${SWAP_SIZE_MB:-2048}"
MIN_MEM_KB_FOR_SWAP="${MIN_MEM_KB_FOR_SWAP:-3145728}"

cd "$PROJECT_DIR"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Error: compose file not found: $COMPOSE_FILE"
  exit 1
fi

if [[ -f "$REMOTE_ENV_TEMPLATE" ]]; then
  cp "$REMOTE_ENV_TEMPLATE" .env
  echo "Synchronized .env from $REMOTE_ENV_TEMPLATE"
elif [[ ! -f ".env" ]]; then
  echo "Warning: .env not found in project root, compose may use host env"
fi

TOTAL_MEM_KB="$(awk '/MemTotal/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)"
TOTAL_SWAP_KB="$(awk '/SwapTotal/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)"
if [[ "$TOTAL_SWAP_KB" -eq 0 && "$TOTAL_MEM_KB" -lt "$MIN_MEM_KB_FOR_SWAP" ]]; then
  echo "Detected low-memory host without swap; creating ${SWAP_SIZE_MB}MB swap..."
  if command -v fallocate >/dev/null 2>&1; then
    fallocate -l "${SWAP_SIZE_MB}M" "$SWAP_FILE" || true
  fi
  if [[ ! -f "$SWAP_FILE" ]]; then
    dd if=/dev/zero of="$SWAP_FILE" bs=1M count="$SWAP_SIZE_MB"
  fi
  chmod 600 "$SWAP_FILE"
  mkswap "$SWAP_FILE"
  swapon "$SWAP_FILE"
  if ! grep -q "^$SWAP_FILE " /etc/fstab; then
    echo "$SWAP_FILE swap swap defaults 0 0" >> /etc/fstab
  fi
  free -h || true
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker is not installed"
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_COMMAND="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_COMMAND="docker-compose"
else
  echo "Error: neither docker compose plugin nor docker-compose is available"
  exit 1
fi

export DOCKER_BUILDKIT=0
export COMPOSE_DOCKER_CLI_BUILD=0

run_prepare() {
  echo "[prepare] Done. .env synchronized, compose detected, low-memory guard applied."
}

run_build_app() {
  echo "[build-app] Building backend image..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" build app
}

run_build_frontend() {
  echo "[build-frontend] Building frontend image..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" build frontend
}

run_up() {
  echo "[up] Starting all services..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" up -d --remove-orphans
}

run_up_core() {
  echo "[up-core] Starting middleware and app..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" up -d postgres redis rustfs createbuckets app
}

run_up_frontend() {
  echo "[up-frontend] Starting frontend..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" up -d frontend
}

run_status() {
  echo "[status] Service status..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" ps
}

run_logs() {
  echo "[logs] Recent logs (last 80 lines)..."
  $COMPOSE_COMMAND -f "$COMPOSE_FILE" logs --tail 80 || true
}

case "$DEPLOY_MODE" in
  oneclick)
    run_build_app
    run_build_frontend
    run_up
    run_status
    run_logs
    ;;
  prepare)
    run_prepare
    ;;
  build-app)
    run_build_app
    ;;
  build-frontend)
    run_build_frontend
    ;;
  up)
    run_up
    ;;
  up-core)
    run_up_core
    ;;
  up-frontend)
    run_up_frontend
    ;;
  status)
    run_status
    ;;
  logs)
    run_logs
    ;;
  *)
    echo "Error: unsupported deploy mode: $DEPLOY_MODE"
    echo "Supported modes: oneclick|prepare|build-app|build-frontend|up|up-core|up-frontend|status|logs"
    exit 1
    ;;
esac

echo "Deployment on remote host completed."
