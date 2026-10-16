package fun.mntale.blueMapCompass;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the one helper every per-player waypoint display is spawned through
 * makes the entity invisible to every client before it is added to the world.
 * That is the property the leak fix depends on: if the display were ever
 * visible by default, some client would receive it before the owner is shown it.
 *
 * <p>No server is required. The world and entity are recording dynamic proxies
 * built the same lightweight way as {@link BannerMarkerStorageTest}.
 */
class WaypointManagerSpawnTest {

    @Test
    void hidesTheDisplayBeforeRunningConfiguration() {
        List<String> order = new ArrayList<>();
        BlockDisplay entity = recording(BlockDisplay.class, order);
        World world = capturingWorld(entity, order);

        BlockDisplay result = WaypointManager.spawnHidden(
                world, location(), BlockDisplay.class, display -> order.add("configure"));

        assertSame(entity, result);
        assertEquals(List.of("spawn", "setVisibleByDefault(false)", "configure"), order,
                "the entity must be hidden before it is configured and handed back to the caller");
    }

    @Test
    void textDisplaysAreHiddenByDefaultToo() {
        List<String> order = new ArrayList<>();
        TextDisplay entity = recording(TextDisplay.class, order);
        World world = capturingWorld(entity, order);

        List<Entity> received = new ArrayList<>();
        TextDisplay result = WaypointManager.spawnHidden(world, location(), TextDisplay.class, received::add);

        assertSame(entity, result);
        assertEquals(List.of(entity), received, "the configuration consumer receives the spawned entity");
        assertEquals(List.of("spawn", "setVisibleByDefault(false)"), order,
                "a display must never be added to the world visible by default");
    }

    private static Location location() {
        return new Location(null, 0, 0, 0);
    }

    @SuppressWarnings("unchecked")
    private static <T> T recording(Class<T> type, List<String> log) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("setVisibleByDefault")) {
                        log.add("setVisibleByDefault(" + args[0] + ")");
                    } else {
                        log.add(method.getName());
                    }
                    return defaultValue(method);
                });
    }

    @SuppressWarnings("unchecked")
    private static World capturingWorld(Entity spawned, List<String> log) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("spawn") && args != null && args.length == 3) {
                        log.add("spawn");
                        ((Consumer<Entity>) args[2]).accept(spawned);
                        return spawned;
                    }
                    return defaultValue(method);
                });
    }

    private static Object defaultValue(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        return null;
    }
}
