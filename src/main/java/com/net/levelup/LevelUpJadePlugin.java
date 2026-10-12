package com.net.levelup;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Jade discovers this optional plugin; no common entry point loads Jade classes. */
@WailaPlugin
public final class LevelUpJadePlugin implements IWailaPlugin {
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(Provider.INSTANCE, Mob.class);
    }
    private enum Provider implements IEntityComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() {
            return new ResourceLocation(LevelUp.ID, "event_name");
        }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            var data = accessor.getEntity().getPersistentData().getCompound(ClientMobVisuals.TAG);
            if (!data.getString("name").isEmpty()) tooltip.add(Component.translatable("levelup.jade.event",
                Component.literal(data.getString("name")).withStyle(style -> style.withColor(data.getInt("rgb")))));
        }
    }
}
