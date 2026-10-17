# Awepeeps BlueMap Compass

Maintained fork of [MidnightTale/BlueMap-Compass](https://github.com/MidnightTale/BlueMap-Compass), prepared for the Awepeeps Paper 26.3 test server.

Sneak-right-click a lodestone or banner with any compass (ordinary, lodestone, or recovery) to create a waypoint at that block, automatically track it for yourself, or remove its waypoint if one already exists. Sneak-right-click another block with a compass to open the waypoint menu; sneak-right-click air also opens the menu. Regular right-clicking air does nothing. Rename the compass in an anvil first to choose the waypoint's label; otherwise it uses the block type and coordinates. The waypoint is published on BlueMap and stored by the plugin. In-game waypoint indicators appear only while any compass is held and within the configured maximum distance; no compass durability is consumed, and selecting a marker does **not** teleport the player.

## Requirements

- Paper 26.3 (build 175) and Java 25
- BlueMap — a hard dependency: the plugin will not enable without it
- `bluemapcompass.use` (default: true)

## Install

1. Drop the built jar (see [Build](#build) for the output path) into the server's
   `plugins/` directory.
2. Install and run BlueMap alongside the server; the plugin requires it and will
   not enable without it.
3. Start the server. On first run the plugin creates
   `plugins/AwepeepsBlueMapCompass/config.yml`; edit it as desired and restart.

## Configuration

`plugins/AwepeepsBlueMapCompass/config.yml` controls marker placement and
interaction pacing. Every key, with its shipped default:

| Key | Default | Meaning |
| --- | --- | --- |
| `marker-targets` | `LODESTONE`, `BANNER` | Block types you may sneak-right-click with a compass to create or remove a waypoint. |
| `max-waypoint-distance` | `50` | Maximum distance, in blocks, at which the in-game waypoint indicator is drawn while a compass is held. |
| `interaction-cooldown-ms` | `750` | Per-player debounce, in milliseconds, applied to every compass action: creating, toggling, or removing a waypoint or marker, opening the waypoint menu, and toggling tracking inside that menu. A repeat of the same action inside this window is ignored, which absorbs the duplicate interaction events Bedrock/Geyser clients send for a single press. Actions are tracked in two independent buckets (marker create/toggle/remove and menu open), so opening the menu right after placing a marker still works. `0` disables the debounce; values outside `0-5000` fall back to the default. |

## Waypoint behaviour

- **Per-player visibility.** A tracked waypoint's in-game indicator (its beam
  and label displays) is spawned hidden and configured inside the spawn
  callback, so it is never visible to any player other than the one it is shown
  to — nearby players never see someone else's waypoint indicators. A startup
  sweep also removes any plugin-tagged display left behind by an older build.
- **Removal on logout.** A player's indicator displays are removed when they
  log off; the join-time cleanup alone left displays in the world until the
  next restart. The tracked set is kept, so the indicators come back on the
  player's next join.
- **Interaction cooldown.** `interaction-cooldown-ms` (see above) debounces
  repeated compass actions per player, including the in-menu tracking toggle,
  and the cooldown state is cleared when the player quits.

## Build

Run `bash automation/build-bluemap-compass.sh`. The helper uses a digest-pinned Gradle/JDK 25 container, verifies the Gradle wrapper distribution checksum, normalizes build inputs, and produces a reproducible shaded plugin jar. The deployment repository pins and verifies the artifact SHA-256.

Lodestone waypoint pillars and labels use a warm amber/gold accent; the BlueMap
lodestone icon uses a matching shaded gold lens. Banner waypoints keep their
banner-color pillar and label.

The output is `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.12.jar`.

## Rollback

To roll back a Compass deployment: stop the server cleanly, restore the
previously reviewed jar (from the same verified backup, together with the
matching `plugins/AwepeepsBlueMapCompass/` data directory), revert the pin —
the source commit and jar SHA-256 — in the private deployment repository, and
start the server again.

## Basic in-game acceptance

1. Sneak-right-click a lodestone or banner with a compass to create a waypoint, and sneak-right-click it again to remove the waypoint. Sneak-right-click an unsupported block or air to open the menu.
2. Repeat marker creation and menu opening with ordinary, lodestone, and recovery compasses.
3. Confirm no compass durability is consumed during creation, menu use, or tracking.
4. Select the waypoint; an indicator and distance should appear while any compass is held, only within 50 blocks by default.
5. Put the compass away or move beyond the draw distance; verify the indicator disappears. Hold a compass again or return within range to restore it.
6. Use the GUI toggles to switch between **Show All** and **Show Yours**, and
   between tracking or untracking all waypoints in the selected visibility scope.
7. Select the marker again to stop tracking, or use the GUI track toggle.
8. Confirm marker creation and selection never teleport the player.

The waypoint menu is titled **Awepeeps-BlueMap-Compass**. It shows up to 45
waypoints per page; use the previous/next arrows and page counter in the bottom
row to navigate. Visibility and category controls are grouped on the left,
bulk tracking is beside them, and page navigation and close are grouped on the
right.

## License

The upstream [MidnightTale/BlueMap-Compass](https://github.com/MidnightTale/BlueMap-Compass)
project publishes **no license**. This fork therefore cannot grant one for the
upstream code it is based on: the changes made in this fork are shared here for
reference only. For reuse terms, contact the upstream project. This repository
intentionally has no `LICENSE` file.

## Repository docs

- [AGENTS.md](AGENTS.md) — repository-wide rules for AI coding agents.
- [.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md](.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md) — reproducible local build and review workflow.
- [RELEASING.md](RELEASING.md) — release and hash-pinning steps.
- [CHANGELOG.md](CHANGELOG.md) — version history of this fork.
- [SECURITY.md](SECURITY.md) — how to report a security issue privately.
- [CONTRIBUTING.md](CONTRIBUTING.md) — build/test steps and pull-request expectations.
