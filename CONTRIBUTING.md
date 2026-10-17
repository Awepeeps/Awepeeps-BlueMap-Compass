# Contributing

## Opening a change

- Open a pull request against `main`. Keep one PR to one logical change.
- Wait for the **Build** workflow to pass before requesting review.
- Explain the behaviour change (what a player or operator would notice) in the
  PR description, not just the code change.

## Build and test

- Direct build and test: `./gradlew --no-daemon clean test build`
  (Gradle wrapper 9.1.0, JDK 25).
- Reproducible containerized build: `bash automation/build-bluemap-compass.sh`.
- CI runs two clean builds of the same commit and compares the jar bytes with
  `cmp`; a pull request that makes the build non-deterministic cannot be
  accepted. The run summary publishes the jar SHA-256.
- Keep the jar reproducible: anyone rebuilding the exact merge commit must get
  a jar with the SHA-256 published by the merged Build run. Releases follow
  [RELEASING.md](RELEASING.md).

## House rules

- Add a `CHANGELOG.md` entry for every user-visible change.
- Keep the plugin name, permission node, config keys, and upstream attribution
  stable. If a config key changes, update the config table in `README.md` and
  the bundled `src/main/resources/config.yml` defaults together.
- Keep docs accurate: `README.md` and [AGENTS.md](AGENTS.md) must describe the
  behaviour that actually ships.
- The upstream project publishes no license (see the License note in the
  README); do not add a `LICENSE` file here.
- This is a **public** repository: never add host paths, usernames, IPs, seeds,
  world coordinates, server or instance IDs, logs, config dumps, backups, or
  secrets — in code, docs, tests, or commit messages.

## Bugs and security

- Bugs: open a GitHub issue with a minimal, generic reproduction.
- Security: report privately per [SECURITY.md](SECURITY.md), never in an issue.
