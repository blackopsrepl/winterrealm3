#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GEODATA_DIR="$ROOT_DIR/deploy/game/data/geodata"
PATHNODE_DIR="$ROOT_DIR/deploy/game/data/pathnode"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

SOURCE="${L2J_GEODATA_SOURCE:-}"
FORCE=0
for arg in "$@"; do
	case "$arg" in
		--force) FORCE=1 ;;
		*) SOURCE="$arg" ;;
	esac
done

count_files() {
	local directory="$1" pattern="$2"
	if [[ ! -d "$directory" ]]; then
		echo 0
		return
	fi
	find "$directory" -maxdepth 1 -type f -name "$pattern" | wc -l
}

collect_entries() {
	local path="$1" target="$2"
	local staging_ok=1

	if [[ -d "$path" ]]; then
		find "$path" -type f \( -name '*.l2j' -o -name '*.pn' \) -exec cp {} "$target" \;
		return
	fi

	if [[ ! -f "$path" ]]; then
		echo "Source not found: $path" >&2
		exit 1
	fi

	local extract_dir
	extract_dir="$(mktemp -d)"
	case "${path,,}" in
		*.zip)
			if unzip -oq "$path" -d "$extract_dir"; then
				staging_ok=0
			fi
			;;
		*) : ;;
	esac
	if [[ "$staging_ok" -ne 0 ]]; then
		echo "Unsupported archive type for: $path (use a zip or a directory)" >&2
		rm -rf "$extract_dir"
		exit 1
	fi
	find "$extract_dir" -type f \( -name '*.l2j' -o -name '*.pn' \) -exec cp {} "$target" \;
	rm -rf "$extract_dir"
}

if [[ -z "$SOURCE" ]]; then
	if (( $(count_files "$GEODATA_DIR" '*.l2j') > 0 )); then
		echo "Geodata is already installed in deploy/game/data/geodata."
		echo "Rerun with --force to overwrite, or pass a source to install a different pack."
		exit 0
	fi
	echo "No geodata source given. This repository deliberately does not distribute geodata files."
	echo
	echo "Download an Interlude geodata zip once from any community archive, for example:"
	echo "  https://www.lineage2.org.uk/download/geodata-interlude"
	echo "  https://www.lineage2.org.uk/download/pathnode-interlude"
	echo "  https://sourceforge.net/projects/l2j-geodata/files/"
	echo
	echo "Then install it by running:"
	echo "  scripts/linux/fetch-geodata.sh /path/to/geodata.zip"
	echo "A directory containing the unpacked files works as well."
	exit 2
fi

if [[ "$SOURCE" =~ ^https?:// ]]; then
	download_path="$TEMP_DIR/geodata-pack.zip"
	if ! curl -fSL -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" -o "$download_path" "$SOURCE"; then
		echo "Download failed: $SOURCE" >&2
		echo "Download the pack manually with a browser, then rerun with the local file path." >&2
		exit 1
	fi
	SOURCE="$download_path"
fi

[[ -d "$GEODATA_DIR" ]] || mkdir -p "$GEODATA_DIR"
[[ -d "$PATHNODE_DIR" ]] || mkdir -p "$PATHNODE_DIR"

staging_dir="$(mktemp -d)"
collect_entries "$SOURCE" "$staging_dir"

copied_geodata=0
copied_pathnodes=0

for file in "$staging_dir"/*.l2j; do
	[[ -e "$file" ]] || continue
	base="$(basename "$file")"
	if [[ -e "$GEODATA_DIR/$base" && "$FORCE" -ne 1 ]]; then
		continue
	fi
	cp "$file" "$GEODATA_DIR/$base"
	copied_geodata=$((copied_geodata + 1))
done

for file in "$staging_dir"/*.pn; do
	[[ -e "$file" ]] || continue
	base="$(basename "$file")"
	if [[ -e "$PATHNODE_DIR/$base" && "$FORCE" -ne 1 ]]; then
		continue
	fi
	cp "$file" "$PATHNODE_DIR/$base"
	copied_pathnodes=$((copied_pathnodes + 1))
done

rm -rf "$staging_dir"

total_geodata="$(count_files "$GEODATA_DIR" '*.l2j')"
total_pathnodes="$(count_files "$PATHNODE_DIR" '*.pn')"

if (( total_geodata == 0 && total_pathnodes == 0 )); then
	echo "No .l2j or .pn files found in: $SOURCE" >&2
	exit 1
fi

echo "Installed $copied_geodata new .l2j files into deploy/game/data/geodata ($total_geodata total)"
echo "Installed $copied_pathnodes new .pn files into deploy/game/data/pathnode ($total_pathnodes total)"

if (( total_geodata < 50 )); then
	echo "Warning: fewer than 50 geodata region files installed." >&2
	echo "Check that this pack is a complete Interlude geodata archive." >&2
fi

echo
echo "Next steps:"
echo "  1. Set GeoData = 1 (line of sight and playable movement) or GeoData = 2 (full pathfinding)"
echo "     in deploy/game/config/options.properties (or the generated Linux config template)."
echo "  2. Restart the game server, for example: scripts/linux/podman-up.sh"
