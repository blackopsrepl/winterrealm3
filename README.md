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

- one **host** computer that runs the world: a Windows 10/11 PC or a Linux
  machine, with at least 4 GB of free memory and 5 GB of free disk space;
- a trusted home network shared by the host and the family PCs;
- a compatible Lineage II Interlude client for each player; and
- an Internet connection on the host for the first start, which downloads
  about 1 GB of server software.

The server runs inside **Docker**, a free program that runs ready-made software
packages ("containers") without installing them one by one. Docker brings
everything the server needs, including Java and the database, so you do **not**
need to install Java, MariaDB, or anything else yourself.

This repository deliberately does **not** include a game client, launcher,
client-side patch, or client assets. Each player must use a compatible copy they
are entitled to use and follow the terms that apply to it.

## Start the world on Windows

You do this setup once. After that, starting the world is a double-click.

### 1. Install Docker Desktop

1. Download **Docker Desktop for Windows** from
   <https://www.docker.com/products/docker-desktop/>. It is free for personal
   and family use.
2. Run the installer and keep the suggested options (including **Use WSL 2**).
   If Windows asks to install or update WSL, accept it.
3. Restart the PC when the installer asks.
4. Open **Docker Desktop** from the Start menu. Accept the terms; you can skip
   signing in. Wait until the bottom-left corner shows **Engine running**.

If Docker Desktop reports that virtualization is disabled, it must be switched
on in the PC's firmware settings (often called *Intel VT-x*, *AMD-V*, or *SVM*);
the PC maker's support site explains how for each model.

### 2. Download Winterrealm3

1. On this repository's GitHub page, choose **Code → Download ZIP**.
2. Right-click the downloaded ZIP, choose **Extract All…**, and extract it to a
   plain folder such as `C:\Winterrealm3`. Avoid folders synced by OneDrive,
   such as Desktop or Documents.

### 3. Start the world

Open the Winterrealm3 folder and double-click **`start-windows.cmd`**.

A window opens and walks through the start:

- It starts Docker Desktop if it is not running yet.
- It finds this PC's address on the home network.
- The first time, Windows asks whether to allow a firewall rule so that the
  other PCs at home can connect. Choose **Yes**.
- The first start downloads the server software and prepares the game
  database. Expect 5–15 minutes; later starts take about a minute.

When it finishes, the window shows **Winterrealm3 is running** and the address
to use, for example:

```text
Point each player's game client at:
  Other PCs at home: 192.168.1.50
  This PC:           127.0.0.1
  Ports:             2106 (login) and 7777 (game)
```

Press a key to close the window; the world keeps running in Docker.

If the window warns that the network is **Public**, set your home network to
Private: **Settings → Network & internet → Wi-Fi** (or **Ethernet**) → your
network → **Network profile type → Private**. Then run `start-windows.cmd` again.

### Stop, restart, and logs on Windows

| To… | Double-click |
| --- | --- |
| Stop the world (characters are kept) | `stop-windows.cmd` |
| Start it again | `start-windows.cmd` |
| Watch the server logs (close the window to stop watching) | `logs-windows.cmd` |

Docker Desktop must be running for any of these; `start-windows.cmd` starts it
for you. If the PC has more than one network connection and the detected
address is wrong, open PowerShell in the Winterrealm3 folder (click the File
Explorer address bar, type `powershell`, press Enter) and give the address
yourself:

```powershell
.\start-windows.cmd -HostIp 192.168.1.50 -LanCidr 192.168.1.0/24
```

Find the right address with `ipconfig`: it is the **IPv4 Address** of the
adapter that has a **Default Gateway**.

## Start the world on Linux

1. Install Docker Engine for your distribution by following
   <https://docs.docker.com/engine/install/>, including the Compose plugin.
   Podman with a Compose provider works as well.
2. Allow your user to run Docker without `sudo`, then log out and back in:

   ```bash
   sudo usermod -aG docker "$USER"
   ```

3. Get the project and start the world:

   ```bash
   git clone https://github.com/blackopsrepl/winterrealm3.git
   cd winterrealm3
   scripts/linux/start.sh
   ```

The helper uses Docker when it is running and Podman otherwise; set
`L2J_ENGINE=docker` or `L2J_ENGINE=podman` to choose. It detects the host's
home-network address, prepares the configuration, starts the database, login,
and game services in order, and prints the address for the family PCs. The
first start downloads the server software and imports the game database, which
takes several minutes.

If the detected address is wrong, provide it explicitly:

```bash
L2J_HOST_IP=192.168.1.50 \
L2J_LAN_CIDR=192.168.1.0/24 \
scripts/linux/start.sh
```

If the host runs a firewall such as `firewalld` or `ufw`, allow TCP ports 2106
and 7777 from the home network.

### Stop, restart, and logs on Linux

```bash
# Stop the world without deleting character data
scripts/linux/stop.sh

# Start it again for the next game night
scripts/linux/start.sh

# Follow all logs, or only the game server
scripts/linux/logs.sh
scripts/linux/logs.sh l2j-game

# Check ports and the advertised address (uses nmap when installed)
scripts/linux/doctor.sh
```

## Join from the family PCs

1. Configure each compatible client or launcher to use the server address the
   start helper printed. A player on the host itself can use `127.0.0.1`.
2. Sign in with a new username and password. Automatic account creation is
   enabled, so there is no separate registration step.
3. Choose the shared world, create a character, and meet in game.

Each family member should use a unique username. Keep those credentials within
your household.

To check from a Windows PC that the host is reachable, run these in PowerShell
(replace the address with your host's):

```powershell
Test-NetConnection 192.168.1.50 -Port 2106
Test-NetConnection 192.168.1.50 -Port 7777
```

Both should report `TcpTestSucceeded : True`. If they do but the game still
cannot connect, the client is pointing at a different address.

## Optional GeoData

GeoData and Pathnode files are derived from Lineage II client map data and are
therefore **not** included in this repository. The game server works without
them (`GeoData = 0`: no line-of-sight checks, no pathfinding), so this step is
optional.

To install them on the host:

1. Download an Interlude geodata zip once from any community archive, for
   example the
   [Lineage 2 Download Centre geodata entry](https://www.lineage2.org.uk/download/geodata-interlude)
   or the [L2J geodata project files](https://sourceforge.net/projects/l2j-geodata/files/).
2. Run the installer helper, pointing it at the downloaded zip (or at an
   already unpacked directory). On Windows, from PowerShell in the
   Winterrealm3 folder:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fetch-geodata.ps1 -Source C:\path\to\geodata.zip
   ```

   On Linux:

   ```bash
   scripts/linux/fetch-geodata.sh /path/to/geodata.zip
   ```

   The helper places `.l2j` files into `deploy/game/data/geodata/` and `.pn`
   files into `deploy/game/data/pathnode/`, regardless of how the archive
   arranged its folders.
3. Enable the feature level you downloaded data for in
   `deploy/game/config/options.properties`:

   | Setting | Meaning |
   | --- | --- |
   | `GeoData = 0` | Off (default) |
   | `GeoData = 1` | Line-of-sight checks and playable movement |
   | `GeoData = 2` | Full pathfinding for all characters |

4. Restart the world with the start helper so the game server picks up the
   data and the new setting.

## Keep the world at home

- Use addresses from the trusted household LAN.
- Do not add router port forwarding for the L2J ports.
- Keep UPnP port mapping disabled.
- The database is reachable only from the host itself; keep it that way.
- Stop the world when the family is finished playing.

The project intentionally contains no cloud-hosting guide, player-recruitment
flow, monetization system, or Internet exposure instructions.

## For tinkerers and maintainers

`official/` is the editable source of truth. `deploy/` is the runnable packaging
generated from those sources, plus the configuration the start helpers render.

| Path | Purpose |
| --- | --- |
| `official/l2j-server-login` | Login service source, resources, SQL, and Maven wrapper |
| `official/l2j-server-game` | Game service source, resources, SQL, and Maven wrapper |
| `official/l2j-server-datapack` | Interlude game data and Maven packaging |
| `deploy/` | Runnable files used by the home deployments |
| `deploy/templates` | Host-specific configuration overrides, rendered into the ignored `deploy/generated` |
| `docker-compose.yml` | The container stack used on both Windows and Linux |
| `start-windows.cmd`, `stop-windows.cmd`, `logs-windows.cmd` | Double-click entry points for `scripts/windows/*.ps1` |
| `scripts/linux/` | Start, stop, logs, doctor, and geodata helpers for Linux |
| `scripts/` | Build, publish, database, and geodata helpers for Windows |

Configuration edits go in `deploy/login/config` and `deploy/game/config`; the
start helpers copy them into `deploy/generated` and apply the host-specific
templates on every start.

### Rebuild the server from source on Windows

Only needed when you change the Java source under `official/`. Running the
world never needs a local Java installation.

1. Install the Java 25 JDK. In PowerShell:

   ```powershell
   winget install EclipseAdoptium.Temurin.25.JDK
   ```

   Or download the Windows installer (`.msi`) for **JDK 25** from
   <https://adoptium.net/temurin/releases/?version=25> and keep the default
   options. Open a new PowerShell window afterwards. The build script finds the
   JDK through `JAVA_HOME` or the default Temurin install folder; pass
   `-JavaHome` to use another location.

2. Build the login, game, and datapack modules, publish the resulting archives
   into `deploy/`, and recreate the L2J containers:

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
