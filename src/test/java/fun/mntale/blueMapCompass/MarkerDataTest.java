package fun.mntale.blueMapCompass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the pure data carrier used across the plugin. No server is required.
 */
class MarkerDataTest {

    private static MarkerData sample() {
        return new MarkerData(
                "marker-id", "Home", "banner", "world", 12, 64, -8,
                "group-id", "player", "owner-uuid", "BLUE"
        );
    }

    @Test
    void exposesEveryComponent() {
        MarkerData marker = sample();

        assertEquals("marker-id", marker.id());
        assertEquals("Home", marker.name());
        assertEquals("banner", marker.type());
        assertEquals("world", marker.world());
        assertEquals(12, marker.x());
        assertEquals(64, marker.y());
        assertEquals(-8, marker.z());
        assertEquals("group-id", marker.groupId());
        assertEquals("player", marker.placedBy());
        assertEquals("owner-uuid", marker.ownerUuid());
        assertEquals("BLUE", marker.color());
    }

    @Test
    void equalityIsValueBased() {
        assertEquals(sample(), sample());
        assertEquals(sample().hashCode(), sample().hashCode());

        MarkerData other = new MarkerData(
                "marker-id", "Home", "banner", "world", 12, 64, -8,
                "group-id", "player", "owner-uuid", "RED"
        );
        assertNotEquals(sample(), other);
    }

    @Test
    void allowsNullOptionalFields() {
        MarkerData marker = new MarkerData(
                "id", "label", "lodestone", "world_nether", 0, 0, 0,
                "awepeeps-waypoints", null, null, null
        );

        assertNull(marker.placedBy());
        assertNull(marker.ownerUuid());
        assertNull(marker.color());
    }
}
