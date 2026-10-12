package fun.mntale.blueMapCompass;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Banner;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.meta.ItemMeta;

import com.tcoded.folialib.FoliaLib;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public final class BlueMapCompass extends JavaPlugin implements Listener {

    public static BlueMapCompass instance;
    public static FoliaLib foliaLib;
    public BannerMarkerStorage bannerMarkerStorage;
    public BannerMarkerListener bannerMarkerListener;
    public MarkerGUI markerGUI;
    public static boolean debug = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        foliaLib = new FoliaLib(this);
        BlueMapIntegration.initialize(instance);
        bannerMarkerStorage = new BannerMarkerStorage(this);
        bannerMarkerListener = new BannerMarkerListener();
        bannerMarkerListener.restoreAllMarkers();
        markerGUI = new MarkerGUI();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(markerGUI, this);
        getServer().getPluginManager().registerEvents(bannerMarkerListener, this);
        // Start global waypoint update task
        WaypointManager.startGlobalWaypointTask(this);
    }

    @Override
    public void onDisable() {
        // Stop global waypoint update task
        WaypointManager.stopGlobalWaypointTask();
    }

    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (event.getHand() == EquipmentSlot.OFF_HAND && isCompass(mainHand)) return;
        if (!player.hasPermission("bluemapcompass.use")) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isCompass(item)) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
                    org.bukkit.block.Block target = event.getClickedBlock();
                    if (target == null) return;
                    if (!isAllowedMarkerTarget(target)) {
                        player.sendMessage(net.kyori.adventure.text.Component.text(
                                "This block is not configured as a waypoint target."));
                        return;
                    }
                    event.setCancelled(true);
                    ItemMeta meta = item.getItemMeta();
                    String markerName = meta != null && meta.hasCustomName() && meta.customName() != null
                            ? PlainTextComponentSerializer.plainText().serialize(meta.customName()).trim()
                            : "";
                    if (markerName.isBlank()) {
                        markerName = (target.getState() instanceof Banner ? "Banner" : "Lodestone")
                                + " at " + target.getX() + ", " + target.getY() + ", " + target.getZ();
                    }
                    bannerMarkerListener.createMarker(target, player, markerName);
                    return;
                }
                if (event.getAction() == Action.RIGHT_CLICK_AIR && !player.isSneaking()) {
                    event.setCancelled(true);
                    MarkerGUI.openFor(player);
                }
            }
        }
    }

    public static boolean isCompass(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.COMPASS || type == Material.RECOVERY_COMPASS;
    }

    private boolean isAllowedMarkerTarget(org.bukkit.block.Block block) {
        java.util.List<String> allowed = getConfig().getStringList("marker-targets").stream()
                .map(String::toUpperCase)
                .toList();
        return (block.getType() == Material.LODESTONE && allowed.contains("LODESTONE"))
                || (block.getState() instanceof Banner && allowed.contains("BANNER"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Set<String> tracked = fun.mntale.blueMapCompass.WaypointManager.getTrackedMarkers(player);
        List<BannerMarkerStorage.BannerMarkerData> allBannerMarkers = fun.mntale.blueMapCompass.BlueMapCompass.instance.bannerMarkerStorage.getAllMarkers();
        Set<String> validMarkerIds = new java.util.HashSet<>();
        for (BannerMarkerStorage.BannerMarkerData marker : allBannerMarkers) validMarkerIds.add(marker.markerId);

        Set<String> newTracked = new java.util.HashSet<>();
        for (String markerId : tracked) {
            if (validMarkerIds.contains(markerId)) {
                newTracked.add(markerId);
            } else {
                fun.mntale.blueMapCompass.WaypointManager.removeWaypointDisplayOnly(player, markerId);
            }
        }
        fun.mntale.blueMapCompass.WaypointManager.setTrackedMarkers(player, newTracked);
        // No display logic here; global task will handle display restoration
        // Per-player display cleanup after join (delayed, Folia region task)
        BlueMapCompass.foliaLib.getScheduler().runAtEntityLater(player, t -> {
            UUID uuid = player.getUniqueId();
            for (org.bukkit.World world : getServer().getWorlds()) {
                for (org.bukkit.entity.BlockDisplay entity : world.getEntitiesByClass(org.bukkit.entity.BlockDisplay.class)) {
                    String playerId = entity.getPersistentDataContainer().get(fun.mntale.blueMapCompass.WaypointManager.DISPLAY_PLAYER_KEY, org.bukkit.persistence.PersistentDataType.STRING);
                    if (playerId != null && playerId.equals(uuid.toString())) {
                        BlueMapCompass.foliaLib.getScheduler().runAtEntity(entity, td -> entity.remove());
                    }
                }
                for (org.bukkit.entity.TextDisplay entity : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                    String playerId = entity.getPersistentDataContainer().get(fun.mntale.blueMapCompass.WaypointManager.DISPLAY_PLAYER_KEY, org.bukkit.persistence.PersistentDataType.STRING);
                    if (playerId != null && playerId.equals(uuid.toString())) {
                        BlueMapCompass.foliaLib.getScheduler().runAtEntity(entity, td -> entity.remove());
                    }
                }
            }
        }, 60L);
    }
}
