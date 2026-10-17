package fun.mntale.blueMapCompass;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure per-player interaction debounce. A manually advanced
 * monotonic clock is injected so the window boundaries can be asserted exactly,
 * with no server and no real sleeping.
 */
class InteractionCooldownTest {

    private final AtomicLong clock = new AtomicLong(1_000L);

    private InteractionCooldown newCooldown(long windowMs) {
        return new InteractionCooldown(windowMs, clock::get);
    }

    @Test
    void firstCallIsAllowed() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
    }

    @Test
    void repeatWithinWindowIsDropped() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        clock.addAndGet(749);
        assertFalse(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER),
                "a repeat 749ms into a 750ms window is a duplicate");
    }

    @Test
    void repeatAtWindowBoundaryIsAllowed() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        clock.addAndGet(750);
        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER),
                "the window is elapsed exactly at the cooldown boundary");
    }

    @Test
    void afterWindowIsAllowed() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        clock.addAndGet(5_000);
        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
    }

    @Test
    void bucketsAreIndependentPerPlayer() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(first, InteractionCooldown.Action.MARKER));
        assertTrue(cooldown.tryAcquire(second, InteractionCooldown.Action.MARKER),
                "one player's cooldown must not block another player");
        assertFalse(cooldown.tryAcquire(first, InteractionCooldown.Action.MARKER));
    }

    @Test
    void bucketsAreIndependentPerActionKind() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.GUI),
                "opening the menu right after placing a marker must still work");
        assertFalse(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        assertFalse(cooldown.tryAcquire(player, InteractionCooldown.Action.GUI));
    }

    @Test
    void zeroWindowDisablesTheDebounce() {
        InteractionCooldown cooldown = newCooldown(0);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        assertEquals(0, cooldown.trackedPlayerCount(), "a disabled debounce keeps no state");
    }

    @Test
    void negativeWindowIsTreatedAsDisabled() {
        InteractionCooldown cooldown = newCooldown(-1);
        UUID player = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
        assertTrue(cooldown.tryAcquire(player, InteractionCooldown.Action.MARKER));
    }

    @Test
    void clearOnQuitDropsOnlyThatPlayersState() {
        InteractionCooldown cooldown = newCooldown(750);
        UUID leaver = UUID.randomUUID();
        UUID stayer = UUID.randomUUID();

        assertTrue(cooldown.tryAcquire(leaver, InteractionCooldown.Action.MARKER));
        assertTrue(cooldown.tryAcquire(stayer, InteractionCooldown.Action.MARKER));
        assertEquals(2, cooldown.trackedPlayerCount());

        cooldown.clear(leaver);

        assertEquals(1, cooldown.trackedPlayerCount(), "the map must not grow with departed players");
        assertTrue(cooldown.tryAcquire(leaver, InteractionCooldown.Action.MARKER),
                "a rejoining player starts with a fresh window");
        assertFalse(cooldown.tryAcquire(stayer, InteractionCooldown.Action.MARKER),
                "the remaining player's cooldown is untouched");
    }

    @Test
    void clearAllDropsEveryPlayer() {
        InteractionCooldown cooldown = newCooldown(750);
        cooldown.tryAcquire(UUID.randomUUID(), InteractionCooldown.Action.GUI);
        cooldown.tryAcquire(UUID.randomUUID(), InteractionCooldown.Action.MARKER);

        cooldown.clearAll();

        assertEquals(0, cooldown.trackedPlayerCount());
    }
}
