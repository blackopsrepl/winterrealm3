# Repository Guidelines

## Project Structure & Module Organization

This repository keeps the editable Winterrealm3 source (an L2J Interlude fork) and local container deployment files together. Treat `official/` as the source of truth:

- `official/l2j-server-login` - login server Java source, resources, SQL, and Maven wrapper.
- `official/l2j-server-game` - game server Java source, resources, SQL, and Maven wrapper.
- `official/l2j-server-datapack` - datapack Maven packaging and game data resources.
- `deploy/` - runnable deployment tree generated from official build artifacts.
- `deploy/templates` - host-specific config overrides that the start helpers render into the ignored `deploy/generated`.
- `scripts/` - PowerShell and Linux shell helpers for build, publish, database import, optional geodata installation, and restart.
- `scripts/fetch-geodata.ps1` and `scripts/linux/fetch-geodata.sh` place user-provided geodata/pathnode archives into `deploy/game/data/geodata` and `deploy/game/data/pathnode`. Those directories are ignored by git because the files are derived from client data and may not be redistributed in the repository.

Avoid hand-editing generated runtime artifacts in `deploy/` unless the change is deployment-specific configuration.

## Build, Test, and Development Commands

- `docker-compose.yml` is the single stack for Windows (Docker Desktop) and Linux (Docker or Podman). Login listens on `2106`, game on `7777`, and MariaDB on loopback port `3307`. Start it through a helper, not bare `docker compose up`: the helpers render `deploy/generated` first, then build the `winterrealm3-local` image from `official/` via `Dockerfile.l2j` (login and game run that image, not the jars under `deploy/`).
- `start-windows.cmd` / `stop-windows.cmd` / `logs-windows.cmd` wrap `scripts/windows/{start,stop,logs}.ps1` (Windows PowerShell 5.1 compatible); `start.ps1` also manages the firewall rule.
- `scripts/linux/start.sh` renders config and starts the stack; `L2J_ENGINE=docker|podman` overrides engine detection.
- `scripts/linux/logs.sh` follows service logs; pass a service name such as `l2j-game` to narrow output.
- `scripts/linux/stop.sh` stops and removes the stack containers, keeping the database volume.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-official.ps1` builds login, game, and datapack modules in order with Java 25 (`-JavaHome`, `JAVA_HOME`, or the default Temurin 25 folder).
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\publish-official-to-deploy.ps1` publishes built ZIP outputs into `deploy/` without restarting containers.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-and-restart.ps1` rebuilds, publishes, recreates login/game containers, and prints `docker compose ps`.
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-and-restart.ps1 -NoBuild` republishes existing artifacts and restarts containers.

## Coding Style & Naming Conventions

Use the existing Java style in each module: package paths such as `net.sf.l2j...` and `com.l2jserver...`, PascalCase classes, camelCase methods and fields, and explicit imports. Preserve GPL headers in existing Java files. Keep files UTF-8 with LF line endings, matching `.gitattributes`. Maven XML uses tabs in current files; PowerShell scripts use clear, indented blocks and `$ErrorActionPreference = "Stop"`. Linux shell scripts should use `set -euo pipefail`.

## Testing Guidelines

No dedicated test suite is currently present. For Java changes, run the relevant module wrapper command from its directory, for example `.\mvnw.cmd test` or `.\mvnw.cmd install`, then run the top-level build/restart script for integration verification. Name future Java tests `*Test.java` under `src/test/java` and keep fixtures near the module they exercise. For deployment changes, confirm `docker compose ps` and inspect login/game container logs.

## Commit & Pull Request Guidelines

History currently contains a single concise initial commit, so prefer short, descriptive subjects such as `Update game server config publishing`. In pull requests, include the affected module, commands run, deployment impact, linked issue if any, and screenshots or log excerpts when startup behavior changes.
