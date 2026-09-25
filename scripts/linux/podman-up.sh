#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/compose.podman.yml"
PROJECT_NAME="winterrealm3"
DB_CONTAINER="winterrealm3-podman-db"
LOGIN_CONTAINER="winterrealm3-podman-login"

compose() {
	podman compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" "$@"
}

existing_containers="$(podman ps -a --filter name=winterrealm3-podman --format '{{.Names}}' || true)"
if [[ -n "$existing_containers" ]]; then
	echo "Stopping existing L2J Podman containers..."
	compose down
fi

"$ROOT_DIR/scripts/linux/render-podman-config.sh"
set -a
# shellcheck disable=SC1091
source "$ROOT_DIR/deploy/linux/generated/podman.env"
set +a

compose up -d l2j-db

echo "Waiting for MariaDB healthcheck..."
deadline=$((SECONDS + 120))
while true; do
	status="$(podman inspect -f '{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null || true)"
	if [[ "$status" == "healthy" ]]; then
		break
	fi
	if [[ "$status" == "unhealthy" ]]; then
		echo "MariaDB container became unhealthy." >&2
		exit 1
	fi
	if (( SECONDS >= deadline )); then
		echo "Timed out waiting for MariaDB to become healthy." >&2
		exit 1
	fi
	sleep 2
done

echo "Waiting for L2J schemas..."
deadline=$((SECONDS + 180))
stable_checks=0
while true; do
	if podman exec "$DB_CONTAINER" mysql -ul2j -pl2jserver2019 -Nse "SELECT COUNT(*) FROM l2jls.gameservers; SELECT COUNT(*) FROM l2jdb.zone_vertices;" >/dev/null 2>&1; then
		stable_checks=$((stable_checks + 1))
	else
		stable_checks=0
	fi
	if (( stable_checks >= 3 )); then
		break
	fi
	if (( SECONDS >= deadline )); then
		echo "Timed out waiting for imported L2J schemas." >&2
		exit 1
	fi
	sleep 2
done

compose up -d l2j-login

echo "Waiting for login game-server listener..."
deadline=$((SECONDS + 120))
while true; do
	login_logs="$(podman logs "$LOGIN_CONTAINER" 2>&1 || true)"
	if grep -q "Listening for game servers on" <<<"$login_logs"; then
		break
	fi
	status="$(podman inspect -f '{{.State.Status}}' "$LOGIN_CONTAINER" 2>/dev/null || true)"
	if [[ "$status" == "exited" || "$status" == "dead" ]]; then
		echo "Login container stopped before opening the game-server listener." >&2
		exit 1
	fi
	if (( SECONDS >= deadline )); then
		echo "Timed out waiting for login game-server listener." >&2
		exit 1
	fi
	sleep 2
done

compose up -d l2j-game
compose ps

cat <<EOF

Windows client endpoint:
  Server IP: $L2J_HOST_IP
  Login TCP: $L2J_HOST_IP:2106
  Game TCP:  $L2J_HOST_IP:7777

PowerShell reachability checks from the Windows PC:
  Test-NetConnection $L2J_HOST_IP -Port 2106
  Test-NetConnection $L2J_HOST_IP -Port 7777
EOF
