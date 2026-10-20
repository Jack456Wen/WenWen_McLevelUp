package com.net.levelup;

import java.util.HashSet;
import java.util.Set;

final class FreeCameraChunkCheck {
    private record Chunk(int x, int z) { }

    static void run() {
        check(FreeCameraChunks.section(-.01) == -1 && FreeCameraChunks.section(-16) == -1
                && FreeCameraChunks.section(-16.01) == -2 && FreeCameraChunks.section(16) == 1,
                "negative and positive block-to-section boundaries");
        check(moved(0, 0, 0, 0, 2).isEmpty(), "stationary viewpoint does not rebuild");
        Set<Chunk> east = moved(0, 0, 1, 0, 2);
        check(east.size() == 10 && east.contains(new Chunk(2, -2)) && east.contains(new Chunk(3, 2)),
                "eastward motion rebuilds both old and new edge");
        check(moved(0, 0, -1, 0, 2).size() == 10, "westward edge");
        check(moved(0, 0, 0, 1, 2).size() == 10 && moved(0, 0, 0, -1, 2).size() == 10,
                "north and south edges");
        for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1}) {
            check(moved(0, 0, dx, dz, 2).size() == 16, "diagonal edges, deduplicated corner");
        }
        check(moved(0, 0, 1_000_000, -1_000_000, 2).size() == 25, "teleport work bounded to current view");
        check(moved(Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0, 2).size() == 25,
                "uninitialized renderer center does not overflow or scan the gap");
        Set<Chunk> returned = new HashSet<>();
        FreeCameraChunks.returned(0, 0, 2, 1, 0, 2, (x, z) -> returned.add(new Chunk(x, z)));
        check(returned.size() == 5 && returned.contains(new Chunk(3, 0)), "return rebuilds newly visible columns");
        returned.clear();
        FreeCameraChunks.returned(0, 0, 2, 0, 0, 2, (x, z) -> returned.add(new Chunk(x, z)));
        check(returned.isEmpty(), "return to unchanged range does not rebuild");
        FreeCameraChunks.returned(0, 0, 2, 0, 0, 3, (x, z) -> returned.add(new Chunk(x, z)));
        check(returned.size() == 24, "expanded view rebuilds newly exposed ring");
        System.out.println("Passed free camera terrain boundary, return and teleport checks.");
    }

    private static Set<Chunk> moved(int oldX, int oldZ, int x, int z, int radius) {
        Set<Chunk> chunks = new HashSet<>();
        FreeCameraChunks.moved(oldX, oldZ, x, z, radius, (cx, cz) -> {
            check(Math.abs(cx - x) <= radius && Math.abs(cz - z) <= radius, "rebuild stays in current view");
            check(chunks.add(new Chunk(cx, cz)), "each column is marked only once");
        });
        return chunks;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
