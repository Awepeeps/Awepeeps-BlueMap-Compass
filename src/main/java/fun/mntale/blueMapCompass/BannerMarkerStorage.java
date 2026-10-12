package fun.mntale.blueMapCompass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BannerMarkerStorage {
    private final File file;
    private final FileConfiguration config;

    public BannerMarkerStorage(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), "banner-markers.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void addMarker(String world, int x, int y, int z, String name, String color, String markerId, String placerUuid, String placerName) {
        addMarker(world, x, y, z, name, "banner", color, markerId, placerUuid, placerName);
    }

    public void addMarker(String world, int x, int y, int z, String name, String type, String color, String markerId, String placerUuid, String placerName) {
        List<String> list = config.getStringList("banner-markers");
        String entry = world + "," + x + "," + y + "," + z + "," + name.replace(",", "<comma>") + "," + color + "," + markerId + "," + placerUuid + "," + placerName.replace(",", "<comma>") + "," + type;
        list.removeIf(s -> {
            String[] parts = s.split(",", 10);
            return parts.length >= 4 && parts[0].equals(world)
                    && parseCoordinate(parts[1], x) && parseCoordinate(parts[2], y)
                    && parseCoordinate(parts[3], z);
        });
        list.add(entry);
        config.set("banner-markers", list);
        save();
    }

    public void removeMarker(String markerId) {
        List<String> list = config.getStringList("banner-markers");
        list.removeIf(s -> {
            String[] parts = s.split(",", 10);
            return parts.length > 6 && parts[6].equals(markerId);
        });
        config.set("banner-markers", list);
        save();
    }

    public List<BannerMarkerData> removeMarkerByLocation(String world, int x, int y, int z) {
        List<BannerMarkerData> removed = getAllMarkers().stream()
                .filter(marker -> marker.world.equals(world) && marker.x == x && marker.y == y && marker.z == z)
                .toList();
        List<String> list = config.getStringList("banner-markers");
        list.removeIf(s -> {
            String[] parts = s.split(",", 10);
            return parts.length >= 4 &&
                   parts[0].equals(world) &&
                   parseCoordinate(parts[1], x) &&
                   parseCoordinate(parts[2], y) &&
                   parseCoordinate(parts[3], z);
        });
        config.set("banner-markers", list);
        save();
        return removed;
    }

    public List<BannerMarkerData> getAllMarkers() {
        List<BannerMarkerData> result = new ArrayList<>();
        List<String> list = config.getStringList("banner-markers");
        for (String s : list) {
            String[] parts = s.split(",", 10);
            if (parts.length >= 7) {
                try {
                    String world = parts[0];
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    String name = parts[4].replace("<comma>", ",");
                    String color = parts[5];
                    String markerId = parts[6];
                    String placerUuid = parts.length > 7 ? parts[7] : "";
                    String placerName = parts.length > 8 ? parts[8].replace("<comma>", ",") : "";
                    String type = parts.length > 9 ? parts[9] : "banner";
                    result.add(new BannerMarkerData(world, x, y, z, name, color, markerId, placerUuid, placerName, type));
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[BlueMapCompass] Ignoring malformed marker entry: " + s);
                }
            }
        }
        return result;
    }

    private static boolean parseCoordinate(String value, int expected) {
        try {
            return Integer.parseInt(value) == expected;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("[BlueMapCompass] Failed to save marker data: " + e.getMessage());
        }
    }

    public static class BannerMarkerData {
        public final String world;
        public final int x, y, z;
        public final String name;
        public final String color;
        public final String markerId;
        public final String placerUuid;
        public final String placerName;
        public final String type;
        public BannerMarkerData(String world, int x, int y, int z, String name, String color, String markerId, String placerUuid, String placerName, String type) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
            this.color = color;
            this.markerId = markerId;
            this.placerUuid = placerUuid;
            this.placerName = placerName;
            this.type = type;
        }
        public Location getLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z);
        }
    }
} 