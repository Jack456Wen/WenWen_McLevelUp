package com.net.levelup;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(LevelUp.ID)
public final class LevelUp {
    public static final String ID = "levelup";
    public static final Logger LOGGER = LogUtils.getLogger();
    public LevelUp() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Settings.SPEC);
        VisualParticles.TYPES.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        Network.register();
        MinecraftForge.EVENT_BUS.register(new Events());
    }
}
