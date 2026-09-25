# Winterrealm3

<p align="center">
  <img src="docs/assets/hearthwarden-mascot.png" alt="The Hearthwarden, a clockwork wyvern guarding the shared Winterrealm3 world" width="360">
</p>

> One shared Interlude world, hosted at home for family adventures.

Winterrealm3 is a family home server built on the open-source L2J Interlude
stack, packaged as a repeatable home setup. One computer keeps the world
running; family PCs on the same trusted household network join it and play
together.

> **Household scope:** The supported project scope is non-commercial play with
> family on a household LAN. It is not designed, documented, or hardened as an
> Internet-facing service.

## A shared world at home

| For your family | What it means |
| --- | --- |
| One persistent world | Characters and progress remain on the host between game nights. |
| A login for each player | A new username and password create a normal player account on first sign-in. |
| Familiar Interlude gameplay | Explore, quest, craft, form a party, and build your own family story. |
| A setup you can tinker with | The editable L2J source and the runnable home deployment live together. |

## Before game night

You will need:

- one Linux host with Podman for the guided setup, or an existing configured
  Windows installation with Docker Desktop;
- a trusted home network shared by the host and the family PCs;
- a compatible Lineage II Interlude client for each player; and
- Internet access on the host for the initial container-image download.

This repository deliberately does **not** include a game client, launcher,
client-side patch, or client assets. Each player must use a compatible copy they
are entitled to use and follow the terms that apply to it.

## Optional GeoData

GeoData and Pathnode files are derived from Lineage II client map data and are
therefore **not** included in this repository. The game server works without
them (`GeoData = 0`: no line-of-sight checks, no pathfinding), so this step is
optional.

To install them on the game host:

1. Download an Interlude geodata zip once from any community archive, for
   example the
   [Lineage 2 Download Centre geodata entry](https://www.lineage2.org.uk/download/geodata-interlude)
   or the [L2J geodata project files](https://sourceforge.net/projects/l2j-geodata/files/).
2. Run the installer helper, pointing it at the downloaded zip (or at an
   already unpacked directory):

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fetch-geodata.ps1 -Source C:\path\to\geodata.zip
   ```

   ```bash
   scripts/linux/fetch-geodata.sh /path/to/geodata.zip
   ```

The helper places `.l2j` files into `deploy/game/data/geodata/` and `.pn`
files into `deploy/game/data/pathnode/`, regardless of how the archive
arranged its folders. On Windows/Docker deployments run the PowerShell
variant on the host; on the Linux host run the shell helper before
`podman-up.sh` so the mounted files are present at first launch.

3. Enable the feature level you downloaded data for in
   `deploy/game/config/options.properties`:

   | Setting | Meaning |
   | --- | --- |
   | `GeoData = 0` | Off (default) |
   | `GeoData = 1` | Line-of-sight checks and playable movement |
   | `GeoData = 2` | Full pathfinding for all characters |

Geodata must be installed at `data/geodata` and pathnodes at `data/pathnode`
next to the game server, which is exactly where the helper puts them.

## Start the home world on Linux

The guided path uses Podman and a Compose provider. From the repository root:

```bash
scripts/linux/podman-up.sh
```

The helper detects the host's LAN address, prepares local configuration, starts
MariaDB and the L2J services in order, and prints the address for the family
PCs.

If automatic detection does not match your home network, provide the address
explicitly:

```bash
L2J_HOST_IP=192.168.1.50 \
L2J_LAN_CIDR=192.168.1.0/24 \
scripts/linux/podman-up.sh
```

Use your host's real LAN address rather than copying the example unchanged.

### Join from the family PCs

1. Configure each compatible client or launcher to use the host address printed
   by `podman-up.sh`.
2. Sign in with a new username and password. Automatic account creation is
   enabled, so there is no separate registration step.
3. Choose the shared world, create a character, and meet in game.

Each family member should use a unique username. Keep those credentials within
your household.

### Pause, resume, and check the world

```bash
# Follow all logs
scripts/linux/podman-logs.sh

# Follow only the game log
scripts/linux/podman-logs.sh l2j-game

# Stop the world without deleting character data
scripts/linux/podman-down.sh

# Start it again for the next game night
scripts/linux/podman-up.sh
```

With `nmap` installed, the doctor helper checks the two game-facing ports and
prints matching PowerShell tests for a Windows family PC:

```bash
scripts/linux/podman-doctor.sh
```

## Existing Windows setup

The original Docker Desktop workflow remains available for an already
configured Windows installation:

```powershell
docker compose up -d
docker compose ps
```

The Linux helper is currently the guided fresh-host path: it renders the LAN
and container settings automatically. A new Windows host still needs equivalent
database and LAN configuration before the first family login.

## Keep the world at home

- Use addresses from the trusted household LAN.
- Do not add router port forwarding for the L2J or database ports.
- Keep UPnP port mapping disabled.
- Keep MariaDB and all generated credentials away from the Internet.
- Stop the containers when the family is finished playing.

The project intentionally contains no cloud-hosting guide, player-recruitment
flow, monetization system, or Internet exposure instructions.

## For tinkerers and maintainers

`official/` is the editable source of truth. `deploy/` is the runnable packaging
generated from those sources.

| Path | Purpose |
| --- | --- |
| `official/l2j-server-login` | Login service source, resources, SQL, and Maven wrapper |
| `official/l2j-server-game` | Game service source, resources, SQL, and Maven wrapper |
| `official/l2j-server-datapack` | Interlude game data and Maven packaging |
| `deploy/` | Runnable files used by the home deployments |
| `deploy/linux/templates` | Linux-specific configuration rendered for the local host |
| `scripts/` | Build, publish, database, and lifecycle helpers |
| `scripts/fetch-geodata.ps1` | Windows helper that installs downloaded geodata/pathnode packs |
| `docker-compose.yml` | Existing Windows/Docker deployment |
| `compose.podman.yml` | Guided Linux/Podman deployment |

### Build and republish on Windows

Build the login, game, and datapack modules with Java 25, publish the resulting
archives into `deploy/`, and recreate the L2J containers:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-and-restart.ps1
```

Republish existing build artifacts without rebuilding:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-and-restart.ps1 -NoBuild
```

Publish without restarting:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\publish-official-to-deploy.ps1
```

Build only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-official.ps1
```

## Rights and attribution

The source components are derived from the official L2J projects on
[Bitbucket](https://bitbucket.org/l2jserver/) and retain their GNU GPL v3
licenses and original notices. Lineage II names and related rights remain with
their respective owners. This independent project is not affiliated with or
endorsed by NCSOFT or the L2J project.

Read [NOTICE.md](NOTICE.md) before redistributing the repository or its build
outputs.
