# shellcheck shell=bash disable=SC2034
# Shared by the Linux helpers; source it, do not run it.
# Picks the container engine once so every helper drives the same stack.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
PROJECT_NAME="winterrealm3"
HOST_ENV="$ROOT_DIR/deploy/generated/host.env"
DB_CONTAINER="winterrealm3-db"
LOGIN_CONTAINER="winterrealm3-login"
GAME_CONTAINER="winterrealm3-game"

# L2J_ENGINE=docker|podman overrides detection. Otherwise prefer a working
# Docker daemon, then Podman.
select_engine() {
	case "${L2J_ENGINE:-}" in
		docker|podman)
			ENGINE="$L2J_ENGINE"
			;;
		"")
			if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
				ENGINE="docker"
			elif command -v podman >/dev/null 2>&1; then
				ENGINE="podman"
			else
				cat >&2 <<EOF
Neither Docker nor Podman is available.
Install Docker Engine (https://docs.docker.com/engine/install/) and start it,
or install Podman with a Compose provider, then run this helper again.
EOF
				exit 1
			fi
			;;
		*)
			echo "L2J_ENGINE must be docker or podman, not: $L2J_ENGINE" >&2
			exit 1
			;;
	esac

	if ! "$ENGINE" compose version >/dev/null 2>&1; then
		echo "'$ENGINE compose' is not available. Install the Compose plugin for $ENGINE." >&2
		exit 1
	fi
}

compose() {
	"$ENGINE" compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" "$@"
}

load_host_env() {
	if [[ ! -f "$HOST_ENV" ]]; then
		echo "The world has not been prepared yet. Run scripts/linux/start.sh first." >&2
		exit 1
	fi
	set -a
	# shellcheck disable=SC1090
	source "$HOST_ENV"
	set +a
}

# wait_healthy CONTAINER LABEL TIMEOUT_SECONDS
wait_healthy() {
	local container="$1" label="$2" timeout="$3" status deadline
	deadline=$((SECONDS + timeout))
	echo "Waiting for $label..."
	while true; do
		status="$("$ENGINE" inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || true)"
		case "$status" in
			healthy) return 0 ;;
			unhealthy)
				echo "$label reported unhealthy. Recent log:" >&2
				"$ENGINE" logs --tail 40 "$container" >&2 || true
				exit 1
				;;
		esac
		if [[ "$("$ENGINE" inspect -f '{{.State.Status}}' "$container" 2>/dev/null || true)" == "exited" ]]; then
			echo "$label stopped unexpectedly. Recent log:" >&2
			"$ENGINE" logs --tail 40 "$container" >&2 || true
			exit 1
		fi
		if (( SECONDS >= deadline )); then
			echo "Timed out waiting for $label. Inspect it with: scripts/linux/logs.sh" >&2
			exit 1
		fi
		sleep 3
	done
}
