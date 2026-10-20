package com.net.levelup;

import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;

final class FreeCameraAccessCheck {
    static void run() {
        Set<String> tags = new HashSet<>();
        check(!FreeCameraAccess.allowed(true, tags), "new player is locked");
        check(FreeCameraAccess.allowed(false, tags), "disabled restriction permits a new player");
        tags.add("unrelated");
        check(!FreeCameraAccess.allowed(true, tags), "unrelated tag cannot unlock");
        tags.add(FreeCameraAccess.UNLOCK_TAG);
        check(FreeCameraAccess.allowed(true, tags), "tag unlocks next request without relogging");
        check(FreeCameraAccess.allowed(false, tags), "unlocked player also works without restriction");
        tags.remove(FreeCameraAccess.UNLOCK_TAG);
        check(!FreeCameraAccess.allowed(true, tags), "tag removal revokes next activation");
        tags.add(FreeCameraAccess.UNLOCK_TAG.toUpperCase(java.util.Locale.ROOT));
        check(!FreeCameraAccess.allowed(true, tags), "tag matching is exact");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (int id : new int[] {1, 127, 128, Integer.MAX_VALUE}) {
                var request = new FreeCameraRequest(id);
                request.write(buffer);
                check(request.equals(FreeCameraRequest.read(buffer)), "request identity survives packet round trip");
                for (boolean allowed : new boolean[] {false, true}) {
                    var reply = new FreeCameraPermission(id, allowed);
                    reply.write(buffer);
                    check(reply.equals(FreeCameraPermission.read(buffer)), "decision and identity survive round trip");
                }
            }
            var snapshot = new FreeCameraPermission(0, false);
            snapshot.write(buffer);
            check(snapshot.equals(FreeCameraPermission.read(buffer)), "lifecycle snapshot has reserved identity");
        } finally { buffer.release(); }
        System.out.println("Passed free camera restriction, unlock/revoke and authorization packet checks.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
