#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=scripts/linux/lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

select_engine
echo "Using container engine: $ENGINE"

# Containers read their configuration at startup, so a running world is
# stopped before it is re-rendered. Characters live in the database volume.
if [[ -n "$(compose ps -q 2>/dev/null || true)" ]]; then
	echo "Stopping the running world before restarting it..."
	compose down
fi

"$ROOT_DIR/scripts/linux/render-config.sh"
load_host_env
mkdir -p "$ROOT_DIR/deploy/login/log" "$ROOT_DIR/deploy/game/log"

# Start the services one at a time so a failure names the service that
# caused it. The first start imports the game database and takes a few minutes.
compose up -d l2j-db
wait_healthy "$DB_CONTAINER" "the database (the first start imports game data and takes a few minutes)" 900
compose up -d l2j-login
wait_healthy "$LOGIN_CONTAINER" "the login server" 180
compose up -d l2j-game
wait_healthy "$GAME_CONTAINER" "the game world to load" 900
compose ps

cat <<EOF

Winterrealm3 is running.

Point each player's game client at this computer:
  Server address: $L2J_HOST_IP
  Login port:     2106
  Game port:      7777

Check reachability from a Windows PC with PowerShell:
  Test-NetConnection $L2J_HOST_IP -Port 2106
  Test-NetConnection $L2J_HOST_IP -Port 7777

Stop the world with: scripts/linux/stop.sh
EOF
