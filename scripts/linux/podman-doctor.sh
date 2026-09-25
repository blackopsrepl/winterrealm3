#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/compose.podman.yml"
PROJECT_NAME="winterrealm3"
ENV_FILE="$ROOT_DIR/deploy/linux/generated/podman.env"

if [[ ! -f "$ENV_FILE" ]]; then
	echo "Generated Podman config is missing. Run scripts/linux/podman-up.sh first." >&2
	exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo "L2J Linux Podman endpoint"
echo "  Host IP:  $L2J_HOST_IP"
echo "  LAN CIDR: $L2J_LAN_CIDR"
echo

echo "Published ports"
podman compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps
echo

echo "Host socket state"
ss -ltnp "sport = :2106 or sport = :7777" || true
echo

echo "Local TCP reachability"
nmap -Pn -p 2106,7777 "$L2J_HOST_IP"
echo

echo "Advertised game-server addresses"
podman logs --tail 80 winterrealm3-podman-login 2>&1 | grep -E "Updated game server|GameServerAuth|/0\\.0\\.0\\.0/0|/$L2J_LAN_CIDR|$L2J_HOST_IP" || true
echo

cat <<EOF
Run these from the Windows PC:
  Test-NetConnection $L2J_HOST_IP -Port 2106
  Test-NetConnection $L2J_HOST_IP -Port 7777

The Lineage II client must target $L2J_HOST_IP for login. If both tests pass
but the game client still does not connect, the client-side l2.ini/launcher
server address is still pointing somewhere else.
EOF
