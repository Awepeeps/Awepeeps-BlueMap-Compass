# Awepeeps BlueMap Compass

Maintained fork of [MidnightTale/BlueMap-Compass](https://github.com/MidnightTale/BlueMap-Compass), prepared for the Awepeeps Paper 26.3 test server.

Sneak-right-click a lodestone or banner with any compass (ordinary, lodestone, or recovery) to create a waypoint at that block. Sneak-right-click air with any compass to browse waypoints and select one to track or untrack it; regular right-clicking air does nothing. Rename the compass in an anvil first to choose the waypoint's label; otherwise it uses the block type and coordinates. The waypoint is published on BlueMap and stored by the plugin. In-game waypoint indicators appear only while any compass is held and within the configured maximum distance; no compass durability is consumed, and selecting a marker does **not** teleport the player.

## Requirements

- Paper 26.3 (build 175) and Java 25
- BlueMap with its API available
- `bluemapcompass.use` (default: true)

`plugins/AwepeepsBlueMapCompass/config.yml` controls marker placement. By default,
only lodestones and banners are allowed. Waypoint indicators are limited to 512 blocks:

```yaml
marker-targets:
  - LODESTONE
  - BANNER
max-waypoint-distance: 512
```

## Build

Run `bash automation/build-bluemap-compass.sh`. The helper uses a digest-pinned Gradle/JDK 25 container, verifies the Gradle wrapper distribution checksum, normalizes build inputs, and produces a reproducible shaded plugin jar. The deployment repository pins and verifies the artifact SHA-256.

The output is `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.4.jar`.

## Basic in-game acceptance

1. Sneak-right-click a lodestone or banner with a compass to create a waypoint; sneak-right-clicking air opens the menu and regular right-clicking air does nothing.
2. Repeat marker creation and menu opening with ordinary, lodestone, and recovery compasses.
3. Confirm no compass durability is consumed during creation, menu use, or tracking.
4. Select the waypoint; an indicator and distance should appear while any compass is held, only inside the configured maximum draw distance.
5. Put the compass away or move beyond the draw distance; verify the indicator disappears. Hold a compass again or return within range to restore it.
6. Use the GUI toggles to switch between **Show All** and **Show Yours**, and
   between tracking or untracking all waypoints in the selected visibility scope.
7. Select the marker again to stop tracking, or use the GUI track toggle.
8. Confirm marker creation and selection never teleport the player.
