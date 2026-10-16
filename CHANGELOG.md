# Changelog

Notable changes to this fork. Versions use the fork's
`<upstream>-awepeeps.<n>` scheme; the deployment repository pins an exact commit
and artifact hash per release.

## 1.4.1-awepeeps.11

- Remove a player's waypoint beam and label displays when they log off. There
  was a join-time cleanup but no quit handler, so displays stayed in the world
  until the next restart or the player's next join. The tracked set is kept, so
  the waypoints come back on the next join.

## 1.4.1-awepeeps.10

- Fix waypoint displays leaking to nearby players. Each tracked marker's beam
  (`BlockDisplay`) and label (`TextDisplay`) were created with
  `World#spawnEntity`, which sends the entity to every client before the plugin
  could hide it, so the follow-up `Player#showEntity` for the owner had nothing
  left to do and every nearby player saw the waypoint. Both displays are now
  spawned hidden by default through `World#spawn(Location, Class, Consumer)`
  and fully configured inside the spawn callback, before the entity is added to
  the world, so it is never visible to anyone except the player it is then shown
  to. A startup sweep also removes any plugin-tagged display left over from an
  older build.

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
