package com.net.levelup;

final class FreeCameraChunks {
    @FunctionalInterface
    interface Marker { void mark(int x, int z); }

    private FreeCameraChunks() { }

    static int section(double coordinate) {
        return (int) Math.floor(coordinate / 16.0);
    }

    static void moved(int oldX, int oldZ, int x, int z, int radius, Marker marker) {
        if (x == oldX && z == oldZ) return;
        // Clamp to the new view: initialization and large teleports must not
        // iterate across the entire gap between camera positions.
        if (x != oldX) {
            int minX = x > oldX ? Math.max(x - radius, oldX + radius) : x - radius;
            int maxX = x > oldX ? x + radius : Math.min(x + radius, oldX - radius);
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = z - radius; cz <= z + radius; cz++) marker.mark(cx, cz);
            }
        }
        if (z != oldZ) {
            int minZ = z > oldZ ? Math.max(z - radius, oldZ + radius) : z - radius;
            int maxZ = z > oldZ ? z + radius : Math.min(z + radius, oldZ - radius);
            // The X strip above already includes this corner.
            for (int cz = minZ; cz <= maxZ; cz++) {
                for (int cx = x - radius; cx <= x + radius; cx++) {
                    boolean inXStrip = x > oldX ? cx >= oldX + radius : x < oldX && cx <= oldX - radius;
                    if (!inXStrip) marker.mark(cx, cz);
                }
            }
        }
    }

    static void returned(int oldX, int oldZ, int oldRadius, int x, int z, int radius, Marker marker) {
        for (int cz = z - radius; cz <= z + radius; cz++) {
            for (int cx = x - radius; cx <= x + radius; cx++) {
                if (cx < oldX - oldRadius || cx > oldX + oldRadius
                        || cz < oldZ - oldRadius || cz > oldZ + oldRadius) marker.mark(cx, cz);
            }
        }
    }
}
