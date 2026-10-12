# Awepeeps BlueMap Compass

Maintained fork of [MidnightTale/BlueMap-Compass](https://github.com/MidnightTale/BlueMap-Compass), prepared for the Awepeeps Paper 26.3 test server.

Sneak-right-click a lodestone or banner with any compass (ordinary, lodestone, or recovery) to create a waypoint at that block. Rename the compass in an anvil first to choose the waypoint's label; otherwise it uses the block type and coordinates. The waypoint is published on BlueMap and stored by the plugin. Right-click the air with any compass to browse waypoints and select one to track or untrack it. In-game waypoint indicators appear only while any compass is held; no compass durability is consumed, and selecting a marker does **not** teleport the player.

## Requirements

- Paper 26.3 (build 175) and Java 25
- BlueMap with its API available
- `bluemapcompass.use` (default: true)

`plugins/AwepeepsBlueMapCompass/config.yml` controls marker placement. By default,
only lodestones and banners are allowed:

```yaml
marker-targets:
  - LODESTONE
  - BANNER
```

## Build

Run `bash automation/build-bluemap-compass.sh`. The helper uses a digest-pinned Gradle/JDK 25 container, verifies the Gradle wrapper distribution checksum, normalizes build inputs, and produces a reproducible shaded plugin jar. The deployment repository pins and verifies the artifact SHA-256.

The output is `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.3.jar`.

## Basic in-game acceptance

1. Repeat marker creation and menu opening with ordinary, lodestone, and recovery compasses.
2. Confirm no compass durability is consumed during creation, menu use, or tracking.
3. Select the waypoint; an indicator and distance should appear while any compass is held.
4. Put all compasses away; verify the indicator disappears. Hold any compass again to restore it.
5. Select the marker again to stop tracking, or use **Untrack All**.
6. Confirm marker creation and selection never teleport the player.
