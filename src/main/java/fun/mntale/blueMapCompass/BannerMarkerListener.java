package fun.mntale.blueMapCompass;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class BannerMarkerListener implements Listener {
    private static final String MARKER_SET_ID = "awepeeps-waypoints";

    public void createMarker(Block block, Player player, String name) {
        Location location = block.getLocation();
        String worldName = location.getWorld().getName();
        String type = block.getState() instanceof Banner ? "banner" : "lodestone";
        String color = block.getState() instanceof Banner banner ? banner.getBaseColor().name() : "";
        String markerId = "waypoint-" + worldName + "-" + location.getBlockX() + "-"
                + location.getBlockY() + "-" + location.getBlockZ();

        BlueMapCompass.instance.bannerMarkerStorage.addMarker(
                worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                name, type, color, markerId, player.getUniqueId().toString(), player.getName());
        addBlueMapMarker(location, name, type, color, markerId, player.getName());
        WaypointManager.addTrackedWaypoint(player, new MarkerData(
                markerId, name, type, worldName, location.getBlockX(), location.getBlockY(),
                location.getBlockZ(), MARKER_SET_ID, player.getName(),
                player.getUniqueId().toString(), color), BlueMapCompass.instance);
        player.sendMessage(net.kyori.adventure.text.Component.text("Waypoint created: " + name));
    }

    private void addBlueMapMarker(Location location, String name, String type, String color,
                                  String markerId, String owner) {
        BlueMapAPI.getInstance().ifPresent(api ->
                api.getWorld(location.getWorld().getUID()).ifPresent(world ->
                        world.getMaps().forEach(map -> {
                            MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(
                                    MARKER_SET_ID,
                                    key -> MarkerSet.builder().label("Awepeeps Waypoints").build());
                            String detail = "<b>" + name + "</b><br>"
                                    + "<b>Position:</b> " + location.getBlockX() + ", "
                                    + location.getBlockY() + ", " + location.getBlockZ()
                                    + "<br><b>Owner:</b> " + owner;
                            POIMarker.Builder markerBuilder = POIMarker.builder()
                                    .label(name)
                                    .position(new Vector3d(location.getX(), location.getY(), location.getZ()))
                                    .detail(detail)
                                    .maxDistance(100000);
                            if (type.equals("banner")) {
                                String iconUrl = "assets/bluemap/web/images/banners/"
                                        + color.toLowerCase() + ".png";
                                markerBuilder.icon(iconUrl, 12, 48);
                            } else {
                                markerBuilder.icon("assets/bluemap/web/images/awepeeps/lodestone.svg", 16, 40);
                            }
                            markerSet.getMarkers().put(markerId, markerBuilder.build());
                        })));
    }

    public List<BannerMarkerStorage.BannerMarkerData> removeMarkerAt(Block block) {
        String worldName = block.getWorld().getName();
        List<BannerMarkerStorage.BannerMarkerData> removed =
                BlueMapCompass.instance.bannerMarkerStorage.removeMarkerByLocation(
                        worldName, block.getX(), block.getY(), block.getZ());
        if (removed.isEmpty()) return removed;

        BlueMapAPI.getInstance().ifPresent(api ->
                api.getWorld(block.getWorld().getUID()).ifPresent(world ->
                        world.getMaps().forEach(map -> {
                            MarkerSet markerSet = map.getMarkerSets().get(MARKER_SET_ID);
                            if (markerSet != null) {
                                removed.forEach(marker -> markerSet.getMarkers().remove(marker.markerId));
                            }
                        })));

        for (BannerMarkerStorage.BannerMarkerData marker : removed) {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                WaypointManager.removeWaypoint(player, marker.markerId);
            }
        }
        return removed;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        removeMarkerAt(event.getBlock());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this::removeMarkerAt);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(this::removeMarkerAt);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        removeMarkerAt(event.getBlock());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        removeMarkerAt(event.getToBlock());
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        removeMarkerAt(event.getBlock());
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(this::removeMarkerAt);
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        event.getBlocks().forEach(this::removeMarkerAt);
    }

    public void restoreAllMarkers() {
        BlueMapAPI.onEnable(api -> {
            for (BannerMarkerStorage.BannerMarkerData data
                    : BlueMapCompass.instance.bannerMarkerStorage.getAllMarkers()) {
                Location location = data.getLocation();
                if (location == null) continue;
                Optional<BlueMapWorld> blueMapWorld = api.getWorld(location.getWorld().getUID());
                blueMapWorld.ifPresent(world -> world.getMaps().forEach(map -> {
                    MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(
                            MARKER_SET_ID,
                            key -> MarkerSet.builder().label("Awepeeps Waypoints").build());
                    String owner = data.placerName == null || data.placerName.isEmpty()
                            ? data.placerUuid : data.placerName;
                    String detail = "<b>" + data.name + "</b><br>"
                            + "<b>Position:</b> " + data.x + ", " + data.y + ", " + data.z
                            + "<br><b>Owner:</b> " + owner;
                    POIMarker.Builder markerBuilder = POIMarker.builder()
                            .label(data.name)
                            .position(new Vector3d(location.getX(), location.getY(), location.getZ()))
                            .detail(detail)
                            .maxDistance(100000);
                    if ("banner".equalsIgnoreCase(data.type)) {
                        String iconUrl = "assets/bluemap/web/images/banners/"
                                + data.color.toLowerCase() + ".png";
                        markerBuilder.icon(iconUrl, 12, 48);
                    } else {
                        markerBuilder.icon("assets/bluemap/web/images/awepeeps/lodestone.svg", 16, 40);
                    }
                    markerSet.getMarkers().put(data.markerId, markerBuilder.build());
                }));
            }
        });
    }
}
