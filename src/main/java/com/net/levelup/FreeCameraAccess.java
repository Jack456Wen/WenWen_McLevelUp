package com.net.levelup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LevelUp.ID)
public final class FreeCameraAccess {
    static final String UNLOCK_TAG = "levelup_free_camera_unlocked";
    private FreeCameraAccess() { }

    static boolean allowed(ServerPlayer player) {
        return allowed(Settings.FREE_CAMERA_REQUIRE_UNLOCK.get(), player.getTags());
    }

    static boolean allowed(boolean restricted, java.util.Set<String> tags) {
        return !restricted || tags.contains(UNLOCK_TAG);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event); }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event); }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        // Entity command tags are not copied by vanilla restoreFrom on death.
        if (event.getOriginal().getTags().contains(UNLOCK_TAG)) event.getEntity().addTag(UNLOCK_TAG);
    }

    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Network.freeCameraPermission(player, 0);
    }
}
