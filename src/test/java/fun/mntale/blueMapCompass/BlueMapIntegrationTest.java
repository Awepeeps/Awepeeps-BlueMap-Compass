package fun.mntale.blueMapCompass;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for importing BlueMap marker positions as block coordinates. The
 * conversion is a pure function over flow-math vectors, so no server is needed.
 *
 * <p>Markers are published at block centers (a block's integer coordinate plus
 * {@code 0.5} on X and Z, as Spawn Kit does for the world-spawn marker), so the
 * block coordinate is the floor of each component. The previous implementation
 * cast each component directly, which truncates toward zero and shifted every
 * marker on a negative axis one block. The coordinate values here are synthetic.
 */
class BlueMapIntegrationTest {

    @Test
    void floorsPositiveIntegerAndHalfBlockPositions() {
        assertEquals(new Vector3i(100, 64, 12),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(100.5, 64.0, 12.5)));
        assertEquals(new Vector3i(100, 64, 12),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(100.0, 64.0, 12.0)));
    }

    @Test
    void floorsPositiveFractionalPositionsDown() {
        assertEquals(new Vector3i(12, 64, 100),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(12.9, 64.0, 100.1)));
        assertEquals(new Vector3i(0, 64, 0),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(0.25, 64.0, 0.0)));
    }

    @Test
    void floorsNegativeHalfBlockPositionsAwayFromZero() {
        assertEquals(new Vector3i(-100, 64, -1),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(-99.5, 64.0, -0.5)));
        assertEquals(new Vector3i(-100, 64, -1),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(-100.0, 64.0, -1.0)));
    }

    @Test
    void floorsNegativeFractionalPositionsAwayFromZero() {
        assertEquals(new Vector3i(-101, 64, -101),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(-100.9, 64.0, -100.1)));
        assertEquals(new Vector3i(-1, 64, 0),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(-0.1, 64.0, 0.1)));
    }

    @Test
    void floorsInsteadOfTruncatingTowardZeroOnNegativeAxes() {
        // A marker one block past its block center on a negative axis: the
        // direct-cast conversion used to truncate -99.5 to -99 (one block too
        // far toward zero) instead of flooring it to the real block, -100.
        Vector3d markerPosition = new Vector3d(-99.5, 64.0, -0.5);

        Vector3i block = BlueMapIntegration.toBlockCoordinates(markerPosition);

        assertEquals(-100, block.getX());
        assertEquals(-1, block.getZ());
        int truncatedX = (int) markerPosition.getX();
        int truncatedZ = (int) markerPosition.getZ();
        assertEquals(-99, truncatedX, "documents the truncation the fix replaced");
        assertEquals(0, truncatedZ, "documents the truncation the fix replaced");
        assertNotEquals(truncatedX, block.getX());
        assertNotEquals(truncatedZ, block.getZ());
    }

    @Test
    void usesIntegerYForBlockCenterMarkers() {
        // Spawn Kit publishes Y as the block coordinate, not block + 0.5.
        assertEquals(new Vector3i(-100, -60, -100),
                BlueMapIntegration.toBlockCoordinates(new Vector3d(-99.5, -60.0, -99.5)));
    }

    @Test
    void importedMarkerReconstructsItsPillarCenterOnBothAxes() {
        int[] blocks = {-100, -99, -50, -1, 0, 1, 50, 99, 100};
        for (int blockX : blocks) {
            for (int blockZ : blocks) {
                // What a block-center publisher emits (Spawn Kit's convention).
                Vector3d published = new Vector3d(blockX + 0.5, 64.0, blockZ + 0.5);

                Vector3i block = BlueMapIntegration.toBlockCoordinates(published);
                assertEquals(blockX, block.getX());
                assertEquals(64, block.getY());
                assertEquals(blockZ, block.getZ());

                // The plugin rebuilds the pillar center as block + 0.5; it must
                // equal exactly what the publisher wrote on both axes.
                Vector3d pillarCenter = new Vector3d(block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
                assertEquals(published, pillarCenter,
                        "import-to-pillar-center invariant broke at " + blockX + ", " + blockZ);
            }
        }
    }

    @Test
    void truncationWouldHaveBrokenThePillarCenterInvariantOnNegativeAxes() {
        int blockX = -100;
        Vector3d published = new Vector3d(blockX + 0.5, 64.0, blockX + 0.5);

        Vector3d truncatedPillarCenter = new Vector3d((int) published.getX() + 0.5, 64.0, (int) published.getZ() + 0.5);

        assertNotEquals(published, truncatedPillarCenter,
                "the pre-fix (int) cast moved the pillar one block on a negative axis");
    }
}
