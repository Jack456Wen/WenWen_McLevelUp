package com.net.levelup;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Settings {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue NETHER, END, XP;
    public static final ForgeConfigSpec.DoubleValue NETHER_ARMOR, END_ARMOR, NETHER_SPEED, END_SPEED;
    public static final ForgeConfigSpec.BooleanValue DAYS;
    public static final ForgeConfigSpec.IntValue XP_CAP;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("血量和攻击共用随机倍率；护甲随机设置基础值，速度独立抽取增幅倍率。").push("dimensions");
        NETHER = b.defineInRange("netherMaxMultiplier", 3.0, 1.0, 100.0);
        END = b.defineInRange("endMaxMultiplier", 5.0, 1.0, 100.0);
        NETHER_ARMOR = b.comment("下界随机基础护甲上限，非倍率。").defineInRange("netherMaxArmor", 8.0, 0.0, 30.0);
        END_ARMOR = b.comment("末地随机基础护甲上限，非倍率。").defineInRange("endMaxArmor", 8.0, 0.0, 30.0);
        NETHER_SPEED = b.comment("下界独立随机速度倍率上限；1.25 表示随机增幅 0～25%，1 表示不增幅。").defineInRange("netherMaxSpeedMultiplier", 1.25, 1.0, 100.0);
        END_SPEED = b.comment("末地独立随机速度倍率上限；1.25 表示随机增幅 0～25%，1 表示不增幅。").defineInRange("endMaxSpeedMultiplier", 1.25, 1.0, 100.0);
        b.pop().push("rewards");
        XP = b.comment("经验 = 原经验 × 怪物强度 × 此倍率，只有玩家击杀生效。").defineInRange("experienceMultiplier", 1.0, 0.0, 100.0);
        XP_CAP = b.comment("每只强化怪物的经验上限，避免极端倍率产生大量经验球。").defineInRange("maxExperiencePerMob", 10000, 0, 100000);
        b.pop().push("specialDays");
        DAYS = b.comment("特殊日详见 config/levelup/days.json，仅主世界生效。").define("enabled", true);
        b.pop();
        SPEC = b.build();
    }
    private Settings() {}
}
