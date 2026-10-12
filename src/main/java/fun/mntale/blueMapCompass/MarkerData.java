package fun.mntale.blueMapCompass;

public record MarkerData(String id, String name, String type, String world, int x, int y, int z,
                         String groupId, String placedBy, String ownerUuid, String color) {
}