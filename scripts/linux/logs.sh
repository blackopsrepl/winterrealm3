#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=scripts/linux/lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

select_engine
compose logs -f "$@"
