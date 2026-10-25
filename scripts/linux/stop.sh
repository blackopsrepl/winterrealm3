#!/usr/bin/env bash
set -euo pipefail

# shellcheck source=scripts/linux/lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

select_engine
# "down" removes containers but keeps the named database volume.
compose down
echo "Winterrealm3 stopped. Characters and progress are kept."
