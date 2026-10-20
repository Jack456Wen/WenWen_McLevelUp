package com.net.levelup.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Sodium-family renderers (Sodium/Rubidium/Embeddium/Magnesium) overwrite
 * LevelRenderer.setupRender, so FreeCameraSetupRenderMixin cannot apply while
 * one is loaded. Their pipeline already centres terrain on the camera, so the
 * free camera keeps working without the vanilla terrain patches.
 */
public final class LevelUpMixinPlugin implements IMixinConfigPlugin {
    private static final String VANILLA_TERRAIN_MIXIN = "com.net.levelup.mixin.FreeCameraSetupRenderMixin";
    private static final String[] SODIUM_RENDERERS = {"sodium", "rubidium", "embeddium", "magnesium"};

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !(VANILLA_TERRAIN_MIXIN.equals(mixinClassName) && sodiumRendererLoaded());
    }

    private static boolean sodiumRendererLoaded() {
        LoadingModList mods = LoadingModList.get();
        if (mods == null) return false;
        for (String id : SODIUM_RENDERERS) {
            if (mods.getModFileById(id) != null) return true;
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
