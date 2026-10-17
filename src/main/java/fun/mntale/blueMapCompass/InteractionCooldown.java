package fun.mntale.blueMapCompass;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-player debounce shared by every compass interaction that creates, toggles,
 * or removes a waypoint or opens the waypoint menu.
 *
 * <p>Bedrock clients (Switch and friends, through Geyser/Floodgate) routinely
 * emit two interaction events for a single press, and even a real double tap
 * would otherwise create a waypoint and immediately delete it again. Every
 * accepted action is timestamped per player and per {@link Action} bucket, and a
 * second attempt inside the configured window is reported as a duplicate so the
 * caller can drop it.
 *
 * <p>The buckets are independent: opening the menu right after placing a marker
 * is a different action and still works, while a duplicate of the same action is
 * suppressed.
 *
 * <p>This class is pure with respect to Bukkit: it only deals with UUIDs, an
 * injectable monotonic clock, and a millisecond window, so it is fully
 * unit-testable without a server.
 */
public final class InteractionCooldown {

    /** Compass action kinds that are debounced independently of each other. */
    public enum Action {
        /** Creating, toggling, or removing a marker/waypoint. */
        MARKER,
        /** Opening the waypoint menu. */
        GUI
    }

    /** Default window when a caller has nothing better; matches the config default. */
    public static final long DEFAULT_COOLDOWN_MS = 750L;
    /** Inclusive lower bound accepted from configuration. */
    public static final long MIN_COOLDOWN_MS = 0L;
    /** Inclusive upper bound accepted from configuration. */
    public static final long MAX_COOLDOWN_MS = 5000L;

    private final Map<UUID, Map<Action, Long>> lastAccepted;
    private final LongSupplier clock;
    private volatile long cooldownMs;

    /** Creates a cooldown backed by {@link System#nanoTime()} in milliseconds. */
    public InteractionCooldown(long cooldownMs) {
        this(cooldownMs, () -> System.nanoTime() / 1_000_000L);
    }

    /**
     * Creates a cooldown with an injectable monotonic clock, used by tests.
     *
     * @param cooldownMs window in milliseconds; values {@code <= 0} disable the debounce
     * @param clock      monotonic millisecond source; tests advance this manually
     */
    public InteractionCooldown(long cooldownMs, LongSupplier clock) {
        this.cooldownMs = Math.max(0L, cooldownMs);
        this.clock = clock;
        this.lastAccepted = new ConcurrentHashMap<>();
    }

    /**
     * Attempts to claim an action for a player.
     *
     * @return {@code true} when the action may run, {@code false} when it is a
     *         duplicate inside the cooldown window and must be dropped
     */
    public boolean tryAcquire(UUID playerId, Action action) {
        if (cooldownMs <= 0L) return true;
        long now = clock.getAsLong();
        Map<Action, Long> perPlayer = lastAccepted.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>());
        Long previous = perPlayer.get(action);
        if (previous != null && now - previous < cooldownMs) {
            return false;
        }
        perPlayer.put(action, now);
        return true;
    }

    /** Drops all state for a player, called when they quit so the map cannot grow. */
    public void clear(UUID playerId) {
        lastAccepted.remove(playerId);
    }

    /** Drops all state; used on shutdown/reload. */
    public void clearAll() {
        lastAccepted.clear();
    }

    /** Number of players currently holding cooldown state; used by tests. */
    int trackedPlayerCount() {
        return lastAccepted.size();
    }

    public long cooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = Math.max(0L, cooldownMs);
    }
}
