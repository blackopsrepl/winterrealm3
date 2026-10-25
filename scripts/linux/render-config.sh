#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TEMPLATE_DIR="$ROOT_DIR/deploy/templates"
GENERATED_DIR="$ROOT_DIR/deploy/generated"
ROUTE_PROBE="${L2J_ROUTE_PROBE:-1.1.1.1}"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

route_line="$(ip -4 route get "$ROUTE_PROBE" 2>/dev/null | head -n 1 || true)"
route_dev="$(awk '{ for (i = 1; i <= NF; i++) if ($i == "dev") { print $(i + 1); exit } }' <<<"$route_line")"
detected_ip="$(awk '{ for (i = 1; i <= NF; i++) if ($i == "src") { print $(i + 1); exit } }' <<<"$route_line")"
detected_cidr=""

if [[ -n "$route_dev" ]]; then
	detected_cidr="$(ip -o -4 addr show dev "$route_dev" scope global | awk '{ print $4; exit }')"
fi

normalize_ipv4_cidr() {
	local cidr="$1"
	local ip="${cidr%/*}"
	local prefix="${cidr#*/}"
	local a b c d ip_int mask net

	if [[ "$cidr" != */* || "$prefix" -lt 0 || "$prefix" -gt 32 ]]; then
		echo "$cidr"
		return
	fi

	IFS=. read -r a b c d <<<"$ip"
	ip_int=$(( (a << 24) | (b << 16) | (c << 8) | d ))
	if [[ "$prefix" -eq 0 ]]; then
		mask=0
	else
		mask=$(( (0xffffffff << (32 - prefix)) & 0xffffffff ))
	fi
	net=$(( ip_int & mask ))

	printf '%d.%d.%d.%d/%d\n' \
		$(( (net >> 24) & 255 )) \
		$(( (net >> 16) & 255 )) \
		$(( (net >> 8) & 255 )) \
		$(( net & 255 )) \
		"$prefix"
}

host_ip="${L2J_HOST_IP:-$detected_ip}"
lan_cidr="${L2J_LAN_CIDR:-$detected_cidr}"

if [[ -z "$host_ip" || -z "$lan_cidr" ]]; then
	cat >&2 <<EOF
Could not detect this computer's home-network address.
Set L2J_HOST_IP and L2J_LAN_CIDR, for example:
  L2J_HOST_IP=192.168.1.50 L2J_LAN_CIDR=192.168.1.0/24 scripts/linux/start.sh
EOF
	exit 1
fi

lan_cidr="$(normalize_ipv4_cidr "$lan_cidr")"

if [[ -f "$GENERATED_DIR/game/config/hexid.txt" ]]; then
	cp "$GENERATED_DIR/game/config/hexid.txt" "$TEMP_DIR/hexid.txt"
fi

case "$GENERATED_DIR" in
	"$ROOT_DIR"/deploy/generated) rm -rf "$GENERATED_DIR" ;;
	*) echo "Refusing to clear unexpected generated directory: $GENERATED_DIR" >&2; exit 1 ;;
esac

mkdir -p "$GENERATED_DIR/login" "$GENERATED_DIR/game"
cp -R "$ROOT_DIR/deploy/login/config" "$GENERATED_DIR/login/config"
cp -R "$ROOT_DIR/deploy/game/config" "$GENERATED_DIR/game/config"
rm -f "$GENERATED_DIR/game/config/hexid.txt" "$GENERATED_DIR/game/config/ipconfig.xml"
cp -R "$TEMPLATE_DIR/." "$GENERATED_DIR/"
if [[ -f "$TEMP_DIR/hexid.txt" ]]; then
	cp "$TEMP_DIR/hexid.txt" "$GENERATED_DIR/game/config/hexid.txt"
fi

escape_sed() {
	printf '%s' "$1" | sed 's/[&|]/\\&/g'
}

host_ip_escaped="$(escape_sed "$host_ip")"
lan_cidr_escaped="$(escape_sed "$lan_cidr")"

while IFS= read -r -d '' file; do
	sed -i \
		-e "s|__L2J_HOST_IP__|$host_ip_escaped|g" \
		-e "s|__L2J_LAN_CIDR__|$lan_cidr_escaped|g" \
		"$file"
done < <(find "$GENERATED_DIR" -type f -print0)

echo "Prepared Winterrealm3 configuration:"
echo "  Host IP:  $host_ip"
echo "  LAN CIDR: $lan_cidr"
echo "  Output:   $GENERATED_DIR"

cat > "$GENERATED_DIR/host.env" <<EOF
L2J_HOST_IP=$host_ip
L2J_LAN_CIDR=$lan_cidr
L2J_BIND_IP=$host_ip
EOF
