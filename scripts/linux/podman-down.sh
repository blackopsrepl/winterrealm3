#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

podman compose -f "$ROOT_DIR/compose.podman.yml" -p winterrealm3 down
