# Awepeeps BlueMap Compass

Maintained fork of [MidnightTale/BlueMap-Compass](https://github.com/MidnightTale/BlueMap-Compass), prepared for the Awepeeps Paper 26.3 test server.

Right-click with an ordinary compass to browse markers published by BlueMap. Select a marker to track or untrack a visible waypoint; selecting a marker does **not** teleport the player. The plugin also publishes named banners as BlueMap markers and offers them as waypoints.

## Requirements

- Paper 26.3 (build 175) and Java 25
- BlueMap with its API available
- `bluemapcompass.use` (default: true)

## Build

Run `bash automation/build-bluemap-compass.sh`. The helper uses a digest-pinned Gradle/JDK 25 container, verifies the Gradle wrapper distribution checksum, normalizes build inputs, and produces a reproducible shaded plugin jar. The deployment repository pins and verifies the artifact SHA-256.

The output is `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.1.jar`.

## Basic in-game acceptance

1. Hold a regular compass and right-click to open the BlueMap markers menu.
2. Select a marker; a visible waypoint indicator and distance should appear.
3. Select it again to stop tracking, or use **Untrack All**.
4. Confirm selection never teleports the player.
5. Place a custom-named banner, then confirm its marker is listed and can be tracked.
