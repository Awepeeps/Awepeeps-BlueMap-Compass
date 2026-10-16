# Awepeeps BlueMap Compass

Maintained fork of [MidnightTale/BlueMap-Compass](https://github.com/MidnightTale/BlueMap-Compass), prepared for the Awepeeps Paper 26.3 test server.

Sneak-right-click a lodestone or banner with any compass (ordinary, lodestone, or recovery) to create a waypoint at that block, automatically track it for yourself, or remove its waypoint if one already exists. Sneak-right-click another block with a compass to open the waypoint menu; sneak-right-click air also opens the menu. Regular right-clicking air does nothing. Rename the compass in an anvil first to choose the waypoint's label; otherwise it uses the block type and coordinates. The waypoint is published on BlueMap and stored by the plugin. In-game waypoint indicators appear only while any compass is held and within the configured maximum distance; no compass durability is consumed, and selecting a marker does **not** teleport the player.

## Requirements

- Paper 26.3 (build 175) and Java 25
- BlueMap with its API available
- `bluemapcompass.use` (default: true)

`plugins/AwepeepsBlueMapCompass/config.yml` controls marker placement. By default,
only lodestones and banners are allowed. Waypoint indicators are limited to 50 blocks by default:

```yaml
marker-targets:
  - LODESTONE
  - BANNER
max-waypoint-distance: 50
```

## Build

Run `bash automation/build-bluemap-compass.sh`. The helper uses a digest-pinned Gradle/JDK 25 container, verifies the Gradle wrapper distribution checksum, normalizes build inputs, and produces a reproducible shaded plugin jar. The deployment repository pins and verifies the artifact SHA-256.

Lodestone waypoint pillars and labels use a warm amber/gold accent; the BlueMap
lodestone icon uses a matching shaded gold lens. Banner waypoints keep their
banner-color pillar and label.

The output is `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.11.jar`.

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

## Repository docs

- [AGENTS.md](AGENTS.md) — repository-wide rules for AI coding agents.
- [.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md](.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md) — reproducible local build and review workflow.
- [RELEASING.md](RELEASING.md) — release and hash-pinning steps.
