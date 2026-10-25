#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=scripts/linux/lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

select_engine
load_host_env

echo "Winterrealm3 endpoint ($ENGINE)"
echo "  Host IP:  $L2J_HOST_IP"
echo "  LAN CIDR: $L2J_LAN_CIDR"
echo

echo "Services"
compose ps
echo

echo "Host socket state"
ss -ltnp "sport = :2106 or sport = :7777" || true
echo

if command -v nmap >/dev/null 2>&1; then
	echo "Local TCP reachability"
	nmap -Pn -p 2106,7777 "$L2J_HOST_IP"
	echo
fi

echo "Advertised game-server addresses"
"$ENGINE" logs --tail 80 "$LOGIN_CONTAINER" 2>&1 | grep -E "Updated game server|GameServerAuth|/0\\.0\\.0\\.0/0|/$L2J_LAN_CIDR|$L2J_HOST_IP" || true
echo

cat <<EOF
Run these from a Windows PC:
  Test-NetConnection $L2J_HOST_IP -Port 2106
  Test-NetConnection $L2J_HOST_IP -Port 7777

The Lineage II client must target $L2J_HOST_IP for login. If both tests pass
but the game client still does not connect, the client-side l2.ini/launcher
server address is still pointing somewhere else.
EOF
