---
name: awepeeps-bluemap-compass-build-review
description: Build and review local changes to the Awepeeps BlueMap Compass Paper plugin reproducibly, confirm the exact source commit and jar SHA-256, and hand that pin to the private deployment repository. Use for local build/review of this fork, not for BlueMap server operation.
---

# Awepeeps BlueMap Compass local build and review

Use this when a change to this repository needs a local build, test, and review before the artifact
hash is handed to the private deployment repository. Repository-wide rules: [AGENTS.md](../../../AGENTS.md);
human overview: [README.md](../../../README.md).

## 1. Build from the exact source

- Work in an isolated worktree of this repository so the checked-out source is unambiguous.
- Reproducible build: `bash automation/build-bluemap-compass.sh` (digest-pinned `gradle:9.1.0-jdk25`
  container, Gradle wrapper 9.1.0, JDK 25).
- Direct build: `./gradlew --no-daemon clean test build`.
- Record the source commit (`git rev-parse HEAD`) and the jar SHA-256 together.

## 2. Verify reproducibility

- CI runs `clean test build` twice and compares the jars with `cmp`; confirm both runs agree.
- For a local check, build twice into separate output directories and compare the hashes. A mismatch
  means a non-deterministic input — fix the cause instead of pinning one build's result.

## 3. Run the tests

- `./gradlew test` runs the JUnit 5 suite under `src/test/java/`.
- Marker-coordinate changes must keep the tests meaningful: assert the actual conversion result
  rather than generated wording.

## 4. Review the change

- Confirm marker/waypoint behavior still uses the plugin's own `WaypointManager`/`MarkerData` and the
  native BlueMap API instead of a new abstraction.
- Confirm imported coordinates use flow-math's native `Vector3d#toInt()`, not an `(int)` cast or
  `Vector3d#floor()`.
- Keep the plugin name, permission node, config keys, and upstream attribution unchanged.
- Public repo: no host paths, usernames, IPs, seeds, world coordinates, server IDs, logs, config
  dumps, backups, or secrets.

## 5. Hand off the pin

- The deployment repository pins the immutable merge commit and the jar SHA-256. Do not install,
  publish, or copy the jar into any other repository from here.
