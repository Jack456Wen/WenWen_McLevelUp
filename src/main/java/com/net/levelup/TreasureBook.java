package com.net.levelup;

import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 掉落表里配置 {@code minecraft:enchanted_book} 时自动生效：抽中后随机附上一条「宝藏附魔」，
 * 也就是附魔台刷不到的稀有附魔（经验修补、冰霜行者、灵魂疾行、迅捷潜行，含模组的同类附魔）。
 * 等级用附魔自身的范围，诅咒虽然也算宝藏附魔，但不会当成奖励发出来。
 */
final class TreasureBook {
    /** 附魔是静态注册表，启动后不再变化，缓存一次即可。 */
    private static List<Enchantment> pool;

    static void apply(ItemStack stack, RandomSource random) {
        List<Enchantment> candidates = pool();
        // 一个宝藏附魔都没有时（几乎不可能，除非注册表被改过）退化成一本文没有附魔的空书。
        if (candidates.isEmpty()) return;
        Enchantment enchantment = candidates.get(random.nextInt(candidates.size()));
        EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, level(enchantment, random)));
    }
    private static List<Enchantment> pool() {
        List<Enchantment> cached = pool;
        if (cached == null) {
            cached = ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .filter(enchantment -> enchantment.isTreasureOnly() && !enchantment.isCurse())
                .toList();
            pool = cached;
        }
        return cached;
    }
    private static int level(Enchantment enchantment, RandomSource random) {
        int min = enchantment.getMinLevel();
        int max = enchantment.getMaxLevel();
        return min >= max ? min : random.nextInt(max - min + 1) + min;
    }
    private TreasureBook() {}
}
