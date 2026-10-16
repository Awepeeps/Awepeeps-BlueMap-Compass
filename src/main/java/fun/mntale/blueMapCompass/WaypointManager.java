package fun.mntale.blueMapCompass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.scheduler.BukkitTask;

public class WaypointManager {
    private static final Map<UUID, Map<String, BlockDisplay>> waypoints = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, TextDisplay>> textDisplays = new ConcurrentHashMap<>();
    public static final NamespacedKey TRACKED_MARKERS_KEY = new NamespacedKey("bluemapcompass", "tracked_markers");
    public static final NamespacedKey DISPLAY_PLAYER_KEY = new NamespacedKey("bluemapcompass", "display_player");
    public static final NamespacedKey DISPLAY_MARKER_KEY = new NamespacedKey("bluemapcompass", "display_marker");

    // Helper to map DyeColor to NamedTextColor
    private static final Map<String, NamedTextColor> DYE_TO_NAMED = Map.ofEntries(
        Map.entry("WHITE", NamedTextColor.WHITE),
        Map.entry("ORANGE", NamedTextColor.GOLD),
        Map.entry("MAGENTA", NamedTextColor.LIGHT_PURPLE),
        Map.entry("LIGHT_BLUE", NamedTextColor.AQUA),
        Map.entry("YELLOW", NamedTextColor.YELLOW),
        Map.entry("LIME", NamedTextColor.GREEN),
        Map.entry("PINK", NamedTextColor.LIGHT_PURPLE),
        Map.entry("GRAY", NamedTextColor.GRAY),
        Map.entry("LIGHT_GRAY", NamedTextColor.WHITE),
        Map.entry("CYAN", NamedTextColor.DARK_AQUA),
        Map.entry("PURPLE", NamedTextColor.DARK_PURPLE),
        Map.entry("BLUE", NamedTextColor.BLUE),
        Map.entry("BROWN", NamedTextColor.GOLD),
        Map.entry("GREEN", NamedTextColor.DARK_GREEN),
        Map.entry("RED", NamedTextColor.RED),
        Map.entry("BLACK", NamedTextColor.BLACK)
    );

    // Add the BANNER_TO_HEX map (copy from MarkerGUI):
    private static final Map<String, String> BANNER_TO_HEX = Map.ofEntries(
        Map.entry("white", "#F9FFFE"),
        Map.entry("orange", "#F9801D"),
        Map.entry("magenta", "#C74EBD"),
        Map.entry("light_blue", "#3AB3DA"),
        Map.entry("yellow", "#FED83D"),
        Map.entry("lime", "#80C71F"),
        Map.entry("pink", "#F38BAA"),
        Map.entry("gray", "#474F52"),
        Map.entry("light_gray", "#9D9D97"),
        Map.entry("cyan", "#169C9C"),
        Map.entry("purple", "#8932B8"),
        Map.entry("blue", "#3C44AA"),
        Map.entry("brown", "#835432"),
        Map.entry("green", "#5E7C16"),
        Map.entry("red", "#B02E26"),
        Map.entry("black", "#1D1D21")
    );

    // Global task for updating all waypoints
    private static WrappedTask globalTask;

    public static void startGlobalWaypointTask(JavaPlugin plugin) {
        stopGlobalWaypointTask();
        globalTask = BlueMapCompass.foliaLib.getImpl().runTimer(() -> updateAllWaypoints(plugin), 1L, 2L);
    }

    public static void stopGlobalWaypointTask() {
        if (globalTask != null) {
            globalTask.cancel();
            globalTask = null;
        }
    }

    private static void updateAllWaypoints(JavaPlugin plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<String> tracked = getTrackedMarkers(player);
            removeDisplaysNotTracked(player, tracked);
            if (!hasCompassInHand(player)) {
                removeAllDisplays(player);
                continue;
            }
            for (String markerId : tracked) {
                MarkerData marker = getMarkerDataById(markerId);
                if (marker != null && MarkerGUI.isVisibleTo(player, marker)) {
                    updateOrSpawnWaypoint(player, marker, plugin);
                } else {
                    removeWaypointDisplayOnly(player, markerId);
                }
            }
        }
    }

    private static void removeDisplaysNotTracked(Player player, Set<String> tracked) {
        Set<String> displayed = new HashSet<>();
        Map<String, BlockDisplay> playerDisplays = waypoints.get(player.getUniqueId());
        if (playerDisplays != null) displayed.addAll(playerDisplays.keySet());
        Map<String, TextDisplay> playerTextDisplays = textDisplays.get(player.getUniqueId());
        if (playerTextDisplays != null) displayed.addAll(playerTextDisplays.keySet());
        for (String markerId : displayed) {
            if (!tracked.contains(markerId)) removeWaypointDisplayOnly(player, markerId);
        }
    }

    /**
     * Removes the player's beam and label displays when they leave. The tracked
     * set is kept, so the global task restores the waypoints on their next join.
     */
    public static void onPlayerQuit(Player player) {
        removeAllDisplays(player);
    }

    private static void removeAllDisplays(Player player) {
        Set<String> displayed = new HashSet<>();
        Map<String, BlockDisplay> playerDisplays = waypoints.get(player.getUniqueId());
        if (playerDisplays != null) displayed.addAll(playerDisplays.keySet());
        Map<String, TextDisplay> playerTextDisplays = textDisplays.get(player.getUniqueId());
        if (playerTextDisplays != null) displayed.addAll(playerTextDisplays.keySet());
        for (String markerId : displayed) removeWaypointDisplayOnly(player, markerId);
    }

    private static boolean hasCompassInHand(Player player) {
        return BlueMapCompass.isCompass(player.getInventory().getItemInMainHand())
                || BlueMapCompass.isCompass(player.getInventory().getItemInOffHand());
    }

    // Lookup MarkerData by id using BlueMapIntegration.getMarkers()
    private static MarkerData getMarkerDataById(String markerId) {
        return fun.mntale.blueMapCompass.BlueMapIntegration.getMarkers().stream()
            .filter(m -> m.id().equals(markerId))
            .findFirst().orElse(null);
    }

    /**
     * Spawns a per-player display entity hidden from every client by default.
     * The configuration consumer runs inside the {@link World#spawn(Location, Class, Consumer)}
     * callback, so it (including {@link Entity#setVisibleByDefault(boolean)}) is
     * applied before the entity is added to the world and before any client can
     * be sent it. The caller must reveal it to its owner with
     * {@link Player#showEntity(org.bukkit.plugin.Plugin, Entity)}.
     */
    static <T extends Entity> T spawnHidden(World world, Location location, Class<T> type, Consumer<T> configure) {
        return world.spawn(location, type, entity -> {
            entity.setVisibleByDefault(false);
            configure.accept(entity);
        });
    }

    /**
     * Removes any plugin-owned display entity still present in loaded chunks.
     * Displays are non-persistent, so this is only a safety net for stale state
     * that outlives a plugin reload: displays spawned by an older version were
     * visible to every client, and a reload does not clear already-loaded
     * entities.
     */
    public static void removeStaleDisplays() {
        BlueMapCompass.foliaLib.getScheduler().runNextTick(task -> {
            for (World world : Bukkit.getWorlds()) {
                removeStaleDisplaysOfType(world, BlockDisplay.class);
                removeStaleDisplaysOfType(world, TextDisplay.class);
            }
        });
    }

    private static <T extends Entity> void removeStaleDisplaysOfType(World world, Class<T> type) {
        for (T entity : world.getEntitiesByClass(type)) {
            if (entity.getPersistentDataContainer().has(DISPLAY_PLAYER_KEY, PersistentDataType.STRING)) {
                BlueMapCompass.foliaLib.getScheduler().runAtEntity(entity, removeTask -> entity.remove());
            }
        }
    }

    private static void updateOrSpawnWaypoint(Player player, MarkerData marker, JavaPlugin plugin) {
        // Recalculate world/target
        String worldName = marker.world();
        if (worldName.contains("#")) {
            worldName = worldName.substring(0, worldName.indexOf('#'));
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            removeWaypointDisplayOnly(player, marker.id());
            return;
        }
        Location target = new Location(world, marker.x() + 0.5, marker.y(), marker.z() + 0.5);
        boolean inSameWorld = player.getWorld().getName().equals(world.getName());
        double maxDistance = Math.max(0, plugin.getConfig().getDouble("max-waypoint-distance", 50));
        if (!inSameWorld || player.getLocation().distanceSquared(target) > maxDistance * maxDistance) {
            removeWaypointDisplayOnly(player, marker.id());
            return;
        }
        Map<String, BlockDisplay> playerDisplays = waypoints.get(player.getUniqueId());
        Map<String, TextDisplay> playerTextDisplays = textDisplays.get(player.getUniqueId());
        BlockDisplay activeDisplay = playerDisplays != null ? playerDisplays.get(marker.id()) : null;
        TextDisplay activeTextDisplay = playerTextDisplays != null ? playerTextDisplays.get(marker.id()) : null;
        if (inSameWorld) {
            // If display is missing or dead, respawn it
            if (activeDisplay == null || activeDisplay.isDead() || activeTextDisplay == null || activeTextDisplay.isDead()) {
                removeWaypointDisplayOnly(player, marker.id());
                // SAFETY: Check player and world before spawning
                if (!player.isOnline() || player.getWorld() == null) {
                    return;
                }
                // Spawn new BlockDisplay and TextDisplay using FoliaLib scheduler at player entity
                BlueMapCompass.foliaLib.getScheduler().runAtEntity(player, (spawnTask) -> {
                    Material spawnedBlockMaterial = getWaypointPillarMaterial(marker);
                    // Spawn hidden-by-default so no client sees it before the owner is
                    // shown it. The configuration consumer runs inside the spawn callback,
                    // i.e. before the entity is added to the world.
                    BlockDisplay spawnedDisplay = spawnHidden(player.getWorld(), getBillboardLocation(player, target, 48), BlockDisplay.class, display -> {
                        display.setBlock(Bukkit.createBlockData(spawnedBlockMaterial));
                        display.setBrightness(new Display.Brightness(15, 15));
                        Transformation spawnedT = display.getTransformation();
                        spawnedT.getScale().set(0.15f, (float) (500 - (-64)), 0.15f);
                        display.setTransformation(spawnedT);
                        display.setPersistent(false);
                        display.getPersistentDataContainer().set(DISPLAY_PLAYER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
                        display.getPersistentDataContainer().set(DISPLAY_MARKER_KEY, PersistentDataType.STRING, marker.id());
                        display.setTeleportDuration(0);
                    });
                    player.showEntity(plugin, spawnedDisplay);
                    waypoints.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(marker.id(), spawnedDisplay);
                    
                    // Spawn new TextDisplay
                    Location spawnedTextLoc = getBillboardLocation(player, target, 48);
                    spawnedTextLoc.setY(player.getEyeLocation().getY() + getDeterministicYOffset(marker.id()));
                    TextDisplay spawnedTextDisplay = spawnHidden(player.getWorld(), spawnedTextLoc, TextDisplay.class, textDisplay -> {
                        textDisplay.setBillboard(Display.Billboard.CENTER);
                        textDisplay.setSeeThrough(true);
                        textDisplay.setPersistent(false);
                        textDisplay.setTeleportDuration(0);
                        textDisplay.setViewRange(128);
                        textDisplay.setBrightness(new Display.Brightness(15, 15));
                        double spawnedInitialDist = player.getLocation().distance(target);
                        float spawnedInitialScale = (float)Math.min(8.0, Math.max(1.0, spawnedInitialDist/24.0));
                        textDisplay.setTransformation(new Transformation(textDisplay.getTransformation().getTranslation(), new org.joml.Quaternionf(), new org.joml.Vector3f(spawnedInitialScale, spawnedInitialScale, spawnedInitialScale), new org.joml.Quaternionf()));
                        textDisplay.getPersistentDataContainer().set(DISPLAY_PLAYER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
                        textDisplay.getPersistentDataContainer().set(DISPLAY_MARKER_KEY, PersistentDataType.STRING, marker.id());
                        TextColor spawnedInitialColor = getWaypointTextColor(marker.type(), marker.color());
                        double spawnedDist = player.getLocation().distance(target);
                        Component spawnedText = Component.text(marker.name() + " (" + Math.round(spawnedDist) + "m)", spawnedInitialColor);
                        textDisplay.text(spawnedText);
                    });
                    player.showEntity(plugin, spawnedTextDisplay);
                    textDisplays.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(marker.id(), spawnedTextDisplay);
                    updateWaypointDisplays(player, target, marker.name(), marker.type(), marker.id(), spawnedDisplay, spawnedTextDisplay, marker.color());
                });
                return;
            }

            // Add null check before updating displays
            if (activeDisplay == null || activeTextDisplay == null) return;
            updateWaypointDisplays(player, target, marker.name(), marker.type(), marker.id(), activeDisplay, activeTextDisplay, marker.color());
        }
    }

    private static Material getWaypointPillarMaterial(MarkerData marker) {
        if (marker.type().equalsIgnoreCase("banner")) {
            String colorName = marker.color() != null ? marker.color().toUpperCase() : "GREEN";
            try {
                return Material.valueOf(colorName + "_STAINED_GLASS");
            } catch (IllegalArgumentException e) {
                return Material.GREEN_STAINED_GLASS;
            }
        }
        return Material.ORANGE_STAINED_GLASS;
    }

    private static TextColor getWaypointTextColor(String markerType, String colorName) {
        if (!markerType.equalsIgnoreCase("banner")) {
            return TextColor.fromHexString("#D4A017");
        }
        String dyeName = colorName != null ? colorName.toUpperCase() : "GREEN";
        return DYE_TO_NAMED.getOrDefault(dyeName, NamedTextColor.GREEN);
    }

    public static Set<String> getTrackedMarkers(Player player) {
        String data = player.getPersistentDataContainer().get(TRACKED_MARKERS_KEY, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(data.split(";")));
    }

    public static void setTrackedMarkers(Player player, Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            player.getPersistentDataContainer().remove(TRACKED_MARKERS_KEY);
        } else {
            player.getPersistentDataContainer().set(TRACKED_MARKERS_KEY, PersistentDataType.STRING, String.join(";", ids));
        }
    }

    public static void setWaypoint(Player player, MarkerData marker, JavaPlugin plugin) {
        // Only add markerId to tracked set
        Set<String> tracked = getTrackedMarkers(player);
        if (!tracked.contains(marker.id())) {
            tracked.add(marker.id());
            setTrackedMarkers(player, tracked);
        }
        // No display logic here; global task will handle display spawning/updating
    }

    // Remove only display/task, not from tracked set
    public static void removeWaypointDisplayOnly(Player player, String markerId) {
        Map<String, BlockDisplay> playerDisplays = waypoints.get(player.getUniqueId());
        if (playerDisplays != null) {
            BlockDisplay display = playerDisplays.remove(markerId);
        if (display != null && !display.isDead()) {
            BlueMapCompass.foliaLib.getScheduler().runAtEntity(display, removeTask -> display.remove());
                if (fun.mntale.blueMapCompass.BlueMapCompass.debug) {
                    Bukkit.getLogger().warning("[BlueMapCompass] BlockDisplay removed for player: " + player.getName());
                }
            }
            if (playerDisplays.isEmpty()) waypoints.remove(player.getUniqueId());
        }
        Map<String, TextDisplay> playerTextDisplays = textDisplays.get(player.getUniqueId());
        if (playerTextDisplays != null) {
            TextDisplay textDisplay = playerTextDisplays.remove(markerId);
            if (textDisplay != null && !textDisplay.isDead()) {
                BlueMapCompass.foliaLib.getScheduler().runAtEntity(textDisplay, removeTask -> textDisplay.remove());
                if (fun.mntale.blueMapCompass.BlueMapCompass.debug) {
                    Bukkit.getLogger().warning("[BlueMapCompass] TextDisplay removed for player: " + player.getName());
                }
            }
            if (playerTextDisplays.isEmpty()) textDisplays.remove(player.getUniqueId());
        }
    }

    public static void removeWaypoint(Player player, String markerId) {
        if (fun.mntale.blueMapCompass.BlueMapCompass.debug) {
            Bukkit.getLogger().warning("[BlueMapCompass] removeWaypoint called for player: " + player.getName() + ", markerId: " + markerId);
        }
        Set<String> tracked = getTrackedMarkers(player);
        tracked.remove(markerId);
        setTrackedMarkers(player, tracked);
        // Remove display entity for this markerId
        removeWaypointDisplayOnly(player, markerId);
    }

    private static Location getBillboardLocation(Player player, Location target, double distance) {
        Location playerLoc = player.getEyeLocation();
        Vector toTarget = target.toVector().subtract(playerLoc.toVector()).normalize();
        Vector pos = playerLoc.toVector().add(toTarget.multiply(distance));
        // Always use the target's Y coordinate for BlockDisplay
        return new Location(player.getWorld(), pos.getX(), -64, pos.getZ());
    }

    public static BlockDisplay getWaypoint(Player player, String markerId) {
        Map<String, BlockDisplay> playerDisplays = waypoints.get(player.getUniqueId());
        if (playerDisplays != null) return playerDisplays.get(markerId);
        return null;
    }

    private static void updateWaypointDisplays(Player player, Location target, String markerName, String markerGroupId, String markerIdFinal, BlockDisplay display, TextDisplay textDisplay, String colorName) {
        if (display == null || display.isDead() || textDisplay == null || textDisplay.isDead()) {
            return;
        }
        // --- FIX: Only update if player and target are in the same world ---
        if (!player.getWorld().equals(target.getWorld())) {
            return;
        }
        double distance = player.getLocation().distance(target);
        float scale = (float)Math.min(6.0, Math.max(1.0, distance/24.0));
        // Set interpolation duration based on distance (longer for large moves)
        int durationTicks = (distance > 48) ? 4 : 2;
        if (distance > 48) {
            Location newLoc = getBillboardLocation(player, target, 48);
            newLoc.setY(player.getEyeLocation().getY() + getDeterministicYOffset(markerIdFinal));
            BlueMapCompass.foliaLib.getScheduler().runAtEntity(display, tpTask -> {
                display.setTeleportDuration(durationTicks);
                display.setInterpolationDuration(durationTicks);
                display.teleportAsync(newLoc);
            });
            BlueMapCompass.foliaLib.getScheduler().runAtEntity(textDisplay, td -> {
                textDisplay.setTeleportDuration(durationTicks);
                textDisplay.setInterpolationDuration(durationTicks);
                textDisplay.teleportAsync(newLoc);
                Transformation t = textDisplay.getTransformation();
                t.getScale().set(scale, scale, scale);
                textDisplay.setTransformation(t);
                TextColor color = getWaypointTextColor(markerGroupId, colorName);
                double distNow = player.getLocation().distance(target);
                Component newText = Component.text(markerName + " (" + Math.round(distNow) + "m)", color);
                textDisplay.text(newText);
            });
        } else {
            BlueMapCompass.foliaLib.getScheduler().runAtEntity(display, tpTask -> {
                display.setTeleportDuration(durationTicks);
                display.setInterpolationDuration(durationTicks);
                display.teleportAsync(target);
            });
            Location textTarget = target.clone();
            textTarget.setY(player.getEyeLocation().getY() + getDeterministicYOffset(markerIdFinal));
            BlueMapCompass.foliaLib.getScheduler().runAtEntity(textDisplay, td -> {
                textDisplay.setTeleportDuration(durationTicks);
                textDisplay.setInterpolationDuration(durationTicks);
                textDisplay.teleportAsync(textTarget);
                Transformation t = textDisplay.getTransformation();
                t.getScale().set(scale, scale, scale);
                textDisplay.setTransformation(t);
                TextColor color = getWaypointTextColor(markerGroupId, colorName);
                double distNow = player.getLocation().distance(target);
                Component newText = Component.text(markerName + " (" + Math.round(distNow) + "m)", color);
                textDisplay.text(newText);
            });
        }
    }

    public static void removeAllWaypoints(Player player) {
        Set<String> tracked = getTrackedMarkers(player);
        for (String markerId : tracked) {
            removeWaypointDisplayOnly(player, markerId);
        }
        setTrackedMarkers(player, new HashSet<>());
        // Remove all BlockDisplay/TextDisplay for this player
        UUID uuid = player.getUniqueId();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(BlockDisplay.class)) {
                String playerId = entity.getPersistentDataContainer().get(DISPLAY_PLAYER_KEY, PersistentDataType.STRING);
                if (playerId != null && playerId.equals(uuid.toString())) {
                    BlueMapCompass.foliaLib.getScheduler().runAtEntity(entity, t -> entity.remove());
                }
            }
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                String playerId = entity.getPersistentDataContainer().get(DISPLAY_PLAYER_KEY, PersistentDataType.STRING);
                if (playerId != null && playerId.equals(uuid.toString())) {
                    BlueMapCompass.foliaLib.getScheduler().runAtEntity(entity, t -> entity.remove());
                }
            }
        }
    }

    private static double getDeterministicYOffset(String markerId) {
        // Deterministic offset in range [-0.5, +0.5] based on markerId hash
        int hash = markerId.hashCode();
        return ((hash % 1000) / 1000.0 - 0.5);
    }

    /**
     * Atomically add a marker to the tracked set.
     */
    public static void addTrackedWaypoint(Player player, MarkerData marker, JavaPlugin plugin) {
        Set<String> tracked = getTrackedMarkers(player);
        if (!tracked.contains(marker.id())) {
            tracked.add(marker.id());
            setTrackedMarkers(player, tracked);
        }
        // No display logic here; global task will handle display spawning/updating
    }
} 