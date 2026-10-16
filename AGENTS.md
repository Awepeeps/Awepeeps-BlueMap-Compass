# AGENTS.md — Awepeeps BlueMap Compass

Scope: this file applies to **every file in this repository**. Human overview: [README.md](README.md).
Repeatable local build/review: [.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md](.agents/skills/awepeeps-bluemap-compass-build-review/SKILL.md).

This is a maintained fork of MidnightTale/BlueMap-Compass. Keep the upstream attribution and license.

## Purpose and layout

- Paper 26.3 / Java 25 plugin: sneak-right-click a compass to create, track, or remove BlueMap waypoints.
- Java sources: `src/main/java/fun/mntale/blueMapCompass/`; unit tests: `src/test/java/fun/mntale/blueMapCompass/`.
- Runtime resources: `src/main/resources/` — `paper-plugin.yml`, `config.yml`, `lodestone-waypoint.svg`.
- Reproducible build helper: `automation/build-bluemap-compass.sh`. Release flow: [RELEASING.md](RELEASING.md).

## Build and test

- Build and test: `./gradlew --no-daemon clean test build` (Gradle wrapper 9.1.0, JDK 25).
- Reproducible containerized build: `bash automation/build-bluemap-compass.sh`.
- Output jar: `build/libs/awepeeps-bluemap-compass-1.4.1-awepeeps.11.jar`.
- Open changes as a pull request against `main`; CI rebuilds twice and compares the jar bytes.

## Native defaults

- Prefer the Paper API and the BlueMap API already used in the code over new wrappers or dependencies.
- Convert imported marker coordinates with flow-math's native `Vector3d#toInt()` (exact double floor).
  Do not use an `(int)` cast (truncates toward zero) or `Vector3d#floor()` (narrows through `float`).

## Privacy and boundaries

- Keep the plugin name, permission node, config keys, and upstream attribution stable.
- This is a **public** repository: never add host paths, usernames, IPs, seeds, world coordinates,
  server or instance IDs, logs, config dumps, backups, or secrets.
- Exact deploy pins (source commit + jar SHA-256) live in the private Awepeeps-Minecraft-Deployment
  repository, never here.
