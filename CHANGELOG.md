# Changelog

Notable changes to this fork. Versions use the fork's
`<upstream>-awepeeps.<n>` scheme; the deployment repository pins an exact commit
and artifact hash per release.

## 1.4.1-awepeeps.9

- Fix marker coordinates imported from BlueMap on negative axes. Imported marker
  positions (for example Spawn Kit's world-spawn marker, published at
  `block + 0.5`) were converted to block coordinates with a plain `(int)` cast,
  which truncates toward zero and moved every marker on a negative axis one block
  (`(int) -99.5 == -99` although the block is `-100`). The conversion now uses
  flow-math's native `Vector3d#toInt()`, whose `(double, double, double)`
  constructor floors each component exactly in double precision, so a marker's
  derived block coordinate and its in-game pillar center match its published
  position on both positive and negative coordinates at every reachable
  magnitude. (`Vector3d#floor()` floors too, but narrows each component through
  `float`, which mis-rounds coordinates beyond `|2^24|` that are still inside the
  vanilla world border, so the direct conversion is used instead.)
