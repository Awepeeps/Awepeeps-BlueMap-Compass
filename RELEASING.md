# Releasing

This repository builds reproducibly: two clean builds of the same commit produce
a jar with the same SHA-256, so the hash published by CI can be pinned with
confidence.

1. Bump `version` in `build.gradle` (and `CHANGELOG.md` if present), then open a
   pull request against `master`.
2. Wait for the **Build** workflow to pass on the pull request, then merge it.
3. Open the successful **Build** run for the merge commit and download the
   `awepeeps-bluemap-compass` artifact; copy the jar file name and its SHA-256
   from the run's job summary.
4. Update the pin in `Awepeeps-Minecraft-Deployment`: set the exact source commit
   (the merge commit SHA) and the jar SHA-256 in
   `config/bluemap-compass-source.env` and `config/plugins.list`.
5. Never pin a moving branch; always pin the immutable merge commit plus hash.
