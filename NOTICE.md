# Rights, Attribution, and Project Scope

This notice records the repository's supported scope and the provenance of its
main components. It does not replace the included license texts or legal advice.

The files in this repository are distributed under the project name
Winterrealm3. The "L2J" and "L2J Server" names below refer to the upstream
projects that Winterrealm3 is derived from.

## Supported scope

This project is designed and documented for non-commercial play among family
members on a trusted household network. That is a project and support boundary,
not an additional restriction on rights granted by an open-source license.

The repository does not distribute a Lineage II client, launcher, client-side
patch, or client assets. Users are responsible for obtaining and using any
compatible client-side software under terms that permit them to do so.

The repository also does not distribute geodata or pathnode files. Those files
are derived from Lineage II client map data and are obtained by each operator
separately; the `scripts/fetch-geodata.ps1` and `scripts/linux/fetch-geodata.sh`
helpers only place user-provided archives into the locations the game server
expects.

## L2J components

The source trees are derived from the official `l2jserver` projects on
[Bitbucket](https://bitbucket.org/l2jserver/). The imported upstream revisions
are:

- [Game service Interlude `496eecad`](https://bitbucket.org/l2jserver/l2j-server-game/src/496eecad4c5e005385b94e84622c33dc58231324/)
- [Datapack Interlude `0d4eace6`](https://bitbucket.org/l2jserver/l2j-server-datapack/src/0d4eace6e4a33eb8a99dae8bda9922e0431d9315/)
- [Login service `ac25e893`](https://bitbucket.org/l2jserver/l2j-server-login/src/ac25e893b2cd85c06920d7f5f34385327c7f8781/)

This repository is a modified distribution. As documented on 13 July 2026,
notable changes include Java 25 and Maven build updates, integrated datapack
packaging, the game-service `//` administrator-command shortcut, and disabling
automatic UPnP port mapping.

The L2J source trees retain their original copyright and license notices. The
included GNU GPL v3 license texts are authoritative for those components:

- [Login service license](official/l2j-server-login/LICENSE.md)
- [Game service license](official/l2j-server-game/LICENSE.md)
- [Datapack license](official/l2j-server-datapack/LICENSE.md)

The `deploy/` tree includes packaged L2J outputs and third-party Java
dependencies. Those dependencies and the container images referenced by the
Compose files retain the copyrights and license terms of their respective
authors.

## Player-bot engineering references

The native player-bot subsystem is original integration work for this fork. Its
design was informed by the following open-source projects:

- [CMaNGOS playerbots](https://github.com/cmangos/playerbots), reviewed and
  adapted at revision `a5b202b14633` for its strategy, trigger, action, value,
  reaction, travel, and group-AI architecture.
- [L2Autobots](https://github.com/Elfocrash/L2Autobots), reviewed at revision
  `f990c61df25e` for Lineage II class behavior, skill conditions, equipment
  data, and operator-interface concepts.

Consulting a project does not by itself mean that its source has been copied.
Every substantive source or data port must be recorded in
`docs/player-bots/import-manifest.md` with its original path, destination,
license notice, and transformation notes.

Do not remove the L2J login notice, copyright notices, license files, or other
attribution when redistributing covered source or build outputs. Anyone
redistributing binaries must also satisfy the corresponding-source and other
requirements of the applicable licenses.

## Names and marks

Lineage II and associated names, marks, and client-side content belong to their
respective owners, including NCSOFT where applicable. They are referenced here
only to describe compatibility and historical game version.

This repository is an adapted Winterrealm3 distribution of the official L2J
Interlude projects. It is not affiliated with, sponsored by, or endorsed by
NCSOFT or the L2J project.
