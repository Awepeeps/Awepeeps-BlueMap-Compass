package fun.mntale.blueMapCompass;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the marker persistence/parsing logic. These exercise real
 * serialisation and lookup behaviour without needing a running server: the only
 * Bukkit type involved is a {@link Plugin} used to locate the data folder, and it
 * is substituted with a lightweight dynamic proxy.
 */
class BannerMarkerStorageTest {

    @TempDir
    Path dataDir;

    private static Plugin plugin(Path dataFolder) {
        File dir = dataFolder.toFile();
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dir;
                    case "getName" -> "BlueMapCompass";
                    case "toString" -> "BlueMapCompass(test)";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> {
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) yield false;
                        if (returnType == int.class) yield 0;
                        if (returnType == long.class) yield 0L;
                        yield null;
                    }
                });
    }

    private BannerMarkerStorage newStorage() {
        return new BannerMarkerStorage(plugin(dataDir));
    }

    @Test
    void addMarkerStoresAndListsEveryField() {
        BannerMarkerStorage storage = newStorage();

        storage.addMarker("world", 10, 64, -20, "Home", "RED", "wp-1", "uuid-1", "Alice");

        List<BannerMarkerStorage.BannerMarkerData> markers = storage.getAllMarkers();
        assertEquals(1, markers.size());
        BannerMarkerStorage.BannerMarkerData marker = markers.get(0);
        assertEquals("world", marker.world);
        assertEquals(10, marker.x);
        assertEquals(64, marker.y);
        assertEquals(-20, marker.z);
        assertEquals("Home", marker.name);
        assertEquals("RED", marker.color);
        assertEquals("wp-1", marker.markerId);
        assertEquals("uuid-1", marker.placerUuid);
        assertEquals("Alice", marker.placerName);
        assertEquals("banner", marker.type, "the short overload defaults the type to banner");
    }

    @Test
    void commasInNamesRoundTrip() {
        BannerMarkerStorage storage = newStorage();

        storage.addMarker("world", 1, 2, 3, "Base, North", "banner", "BLUE",
                "wp-2", "uuid-2", "Lee, Ann");

        BannerMarkerStorage.BannerMarkerData marker = storage.getAllMarkers().get(0);
        assertEquals("Base, North", marker.name);
        assertEquals("Lee, Ann", marker.placerName);
    }

    @Test
    void addingMarkerAtSameLocationReplacesTheExistingOne() {
        BannerMarkerStorage storage = newStorage();

        storage.addMarker("world", 5, 5, 5, "First", "RED", "wp-a", "uuid-a", "Alice");
        storage.addMarker("world", 5, 5, 5, "Second", "GREEN", "wp-b", "uuid-b", "Bob");

        List<BannerMarkerStorage.BannerMarkerData> markers = storage.getAllMarkers();
        assertEquals(1, markers.size(), "only one marker may exist per block location");
        assertEquals("Second", markers.get(0).name);
        assertEquals("wp-b", markers.get(0).markerId);
    }

    @Test
    void removeMarkerByLocationReturnsAndDropsTheMatchingEntries() {
        BannerMarkerStorage storage = newStorage();
        storage.addMarker("world", 7, 8, 9, "Here", "RED", "wp-c", "uuid-c", "Alice");
        storage.addMarker("world", 1, 2, 3, "There", "RED", "wp-d", "uuid-d", "Bob");

        List<BannerMarkerStorage.BannerMarkerData> removed =
                storage.removeMarkerByLocation("world", 7, 8, 9);

        assertEquals(1, removed.size());
        assertEquals("wp-c", removed.get(0).markerId);
        List<BannerMarkerStorage.BannerMarkerData> remaining = storage.getAllMarkers();
        assertEquals(1, remaining.size());
        assertEquals("wp-d", remaining.get(0).markerId);
    }

    @Test
    void removeMarkerByIdRemovesOnlyThatMarker() {
        BannerMarkerStorage storage = newStorage();
        storage.addMarker("world", 1, 1, 1, "Keep", "banner", "WHITE", "keep-id", "u1", "Alice");
        storage.addMarker("world", 2, 2, 2, "Drop", "banner", "WHITE", "drop-id", "u2", "Bob");

        storage.removeMarker("drop-id");

        List<BannerMarkerStorage.BannerMarkerData> markers = storage.getAllMarkers();
        assertEquals(1, markers.size());
        assertEquals("keep-id", markers.get(0).markerId);
        assertTrue(markers.stream().noneMatch(m -> m.markerId.equals("drop-id")));
    }
}
