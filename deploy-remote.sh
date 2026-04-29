#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${1:-docker-compose.yml}"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REMOTE_ENV_TEMPLATE="deploy/.env.ecs.example"

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

echo "[1/3] Rebuilding and starting services..."
$COMPOSE_COMMAND -f "$COMPOSE_FILE" up -d --build --remove-orphans

echo "[2/3] Service status..."
$COMPOSE_COMMAND -f "$COMPOSE_FILE" ps

echo "[3/3] Recent logs (last 80 lines)..."
$COMPOSE_COMMAND -f "$COMPOSE_FILE" logs --tail 80 || true

echo "Deployment on remote host completed."
