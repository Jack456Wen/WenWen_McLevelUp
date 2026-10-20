package com.net.levelup.mixin;

import com.net.levelup.FreeCamera;
import com.net.levelup.FreeCameraRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class FreeCameraLevelRendererMixin {
    @Shadow private int lastCameraChunkX;
    @Shadow private int lastCameraChunkZ;
    @Unique private int levelup$previousX;
    @Unique private int levelup$previousZ;

    // Equivalent to the reference's temporary spectator flag, restricted to
    // terrain setup instead of changing player.isSpectator() for unrelated code.
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"), index = 3, require = 0)
    private boolean levelup$spectatorTerrain(boolean spectator) {
        return FreeCamera.active() || spectator;
    }

    @Inject(method = "setupRender", at = @At("HEAD"), require = 0)
    private void levelup$rememberCenter(CallbackInfo ci) {
        levelup$previousX = lastCameraChunkX;
        levelup$previousZ = lastCameraChunkZ;
    }

    @Unique
    private static Entity levelup$view(LocalPlayer player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        return FreeCamera.active() && camera != null ? camera : player;
    }

    @Redirect(method = "setupRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"), require = 0)
    private double levelup$cameraX(LocalPlayer player) { return levelup$view(player).getX(); }

    @Redirect(method = "setupRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"), require = 0)
    private double levelup$cameraY(LocalPlayer player) { return levelup$view(player).getY(); }

    @Redirect(method = "setupRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"), require = 0)
    private double levelup$cameraZ(LocalPlayer player) { return levelup$view(player).getZ(); }

    // Like the reference, tolerate renderers that replace vanilla terrain setup.
    @Redirect(method = "setupRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewArea;repositionCamera(DD)V"), require = 0)
    private void levelup$rebuildEdges(ViewArea area, double x, double z) {
        area.repositionCamera(x, z);
        FreeCameraRendering.moved(levelup$previousX, levelup$previousZ, x, z);
    }
}
