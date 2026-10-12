package com.net.levelup;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** JSON is read only on server start or explicit reload, never per entity/tick. */
public final class Rules {
    public record Day(long day, double probability, boolean repeatable, String title, String announcement,
                      double health, double attack, double speed, double armor, String loot, double experienceMultiplier,
                      List<AttackEffects.Entry> attackEffects, List<AttackEffects.Entry> playerEffects, EventBgm bgm) {
        // Stable across reordering and changes to probability/effects; these three fields identify an event.
        String historyKey() {
            return UUID.nameUUIDFromBytes((day + "\u0000" + title + "\u0000" + loot)
                .getBytes(StandardCharsets.UTF_8)).toString();
        }
    }
    public record Entry(Item item, int count, double weight, double rarity, double minimumStrength) {}
    public record Snapshot(List<Day> days, Map<String, List<Entry>> tables) {}
    public static volatile Snapshot current = new Snapshot(List.of(), Map.of());
    public static Path directory() { return FMLPaths.CONFIGDIR.get().resolve(LevelUp.ID); }
    public static void reload() throws Exception {
        Path dir = directory();
        Files.createDirectories(dir);
        createDefault(dir.resolve("days.json"), """
            {"days":[{"day":7,"probability":0.15,"repeatable":true,"title":"文文","announcement":"文本文本！","healthMultiplier":3,"attackMultiplier":2,"speedMultiplier":1.3,"armor":8,"lootTable":"wenwen","experienceMultiplier":1,"attackEffects":[],"playerEffects":[],"bgm":"","bgmVolume":1,"bgmDurationSeconds":180}]}
            """);
        createDefault(dir.resolve("loot.json"), """
            {"tables":{
              "dimension":[
                {"item":"minecraft:iron_ingot","count":1,"weight":100,"rarity":0,"minStrength":1},
                {"item":"minecraft:gold_ingot","count":1,"weight":30,"rarity":1,"minStrength":1},
                {"item":"minecraft:diamond","count":1,"weight":3,"rarity":3,"minStrength":2},
                {"item":"minecraft:netherite_scrap","count":1,"weight":1,"rarity":4,"minStrength":3}],
              "wenwen":[
                {"item":"minecraft:gold_ingot","count":1,"weight":50,"rarity":0,"minStrength":1},
                {"item":"minecraft:diamond","count":1,"weight":5,"rarity":2,"minStrength":1}]
            }}
            """);
        List<Day> days = new ArrayList<>();
        Map<String, List<Entry>> tables = new HashMap<>();
        JsonObject loot = read(dir.resolve("loot.json")).getAsJsonObject("tables");
        for (var table : loot.entrySet()) {
            List<Entry> entries = new ArrayList<>();
            for (JsonElement element : table.getValue().getAsJsonArray()) {
                JsonObject e = element.getAsJsonObject();
                int count = integer(e, "count", 1, 1, 64);
                double weight = number(e, "weight", 1, 0.000001, 1e9);
                double rarity = number(e, "rarity", 0, 0, 10);
                double minimumStrength = number(e, "minStrength", 1, 1, 100);
                // 轮空条目：item 为 null，抽中它就表示这次不掉额外物品。
                if (blank(e)) {
                    entries.add(new Entry(null, count, weight, rarity, minimumStrength));
                    continue;
                }
                if (!e.has("item")) throw new IllegalArgumentException("Missing item in loot table: " + table.getKey());
                ResourceLocation id = ResourceLocation.tryParse(e.get("item").getAsString());
                if (id == null || !ForgeRegistries.ITEMS.containsKey(id))
                    throw new IllegalArgumentException("Unknown item: " + e.get("item"));
                entries.add(new Entry(ForgeRegistries.ITEMS.getValue(id), count, weight, rarity, minimumStrength));
            }
            if (entries.isEmpty()) throw new IllegalArgumentException("Empty loot table: " + table.getKey());
            if (entries.stream().noneMatch(e -> e.minimumStrength <= 1))
                throw new IllegalArgumentException("Table needs an entry with minStrength=1: " + table.getKey());
            tables.put(table.getKey(), List.copyOf(entries));
        }
        if (!tables.containsKey("dimension")) throw new IllegalArgumentException("Missing dimension loot table");
        for (JsonElement element : read(dir.resolve("days.json")).getAsJsonArray("days")) {
            JsonObject d = element.getAsJsonObject();
            String table = d.get("lootTable").getAsString();
            if (!tables.containsKey(table)) throw new IllegalArgumentException("Missing loot table: " + table);
            days.add(new Day(integer(d,"day",1,1,Integer.MAX_VALUE), probability(d), repeatable(d),
                d.get("title").getAsString(), d.get("announcement").getAsString(),
                number(d,"healthMultiplier",1,1,100), number(d,"attackMultiplier",1,1,100),
                number(d,"speedMultiplier",1,1,100), number(d,"armor",0,0,1000), table,
                number(d,"experienceMultiplier",1,0,100), AttackEffects.parse(d), PlayerEffects.parse(d), EventBgm.parse(d)));
        }
        current = new Snapshot(List.copyOf(days), Map.copyOf(tables));
    }
    static boolean repeatable(JsonObject day) {
        if (!day.has("repeatable")) return true;
        JsonElement value = day.get("repeatable");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("repeatable must be true or false");
        return value.getAsBoolean();
    }
    static double probability(JsonObject day) {
        if (day.has("repeatEvery"))
            throw new IllegalArgumentException("repeatEvery is no longer supported; replace it with probability (0 to 1)");
        if (!day.has("probability")) throw new IllegalArgumentException("Missing probability (0 to 1)");
        return number(day, "probability", 0, 0, 1);
    }
    public static ItemStack roll(String table, double strength, RandomSource random) {
        List<Entry> entries = current.tables.get(table);
        if (entries == null) return ItemStack.EMPTY;
        Entry entry = pick(entries, strength, random.nextDouble());
        // item 为 null 的轮空条目，或没有可抽取的条目时，都不产生额外掉落。
        if (entry == null || entry.item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(entry.item, entry.count);
        // 附魔书条目自动带一条宝藏附魔：附魔台刷不到的那些（经验修补、冰霜行者、灵魂疾行、迅捷潜行）。
        if (stack.is(Items.ENCHANTED_BOOK)) TreasureBook.apply(stack, random);
        return stack;
    }
    /**
     * 按实际权重挑出条目，{@code random} 取 [0,1)；没有可抽取的条目（门槛都不满足或总权重为 0）返回 null。
     * 抽奖与物品堆构建拆开，方便在没有 Minecraft 启动环境时测门槛与权重。
     */
    static Entry pick(List<Entry> entries, double strength, double random) {
        double total = 0;
        for (Entry e : entries) if (strength >= e.minimumStrength) total += weight(e, strength);
        if (total <= 0) return null;
        double value = random * total;
        for (Entry e : entries) {
            if (strength < e.minimumStrength) continue;
            value -= weight(e, strength);
            if (value < 0) return e;
        }
        return null;
    }
    /** 轮空条目：显式 {@code "blank": true}，或把 item 写成 {@code minecraft:air}（原版里空物品堆就是 air）。 */
    private static boolean blank(JsonObject e) {
        if (e.has("blank")) return e.get("blank").getAsBoolean();
        return e.has("item") && "minecraft:air".equals(e.get("item").getAsString());
    }
    private static double weight(Entry e, double strength) { return BalanceMath.weight(e.weight, e.rarity, strength); }
    private static void createDefault(Path path, String text) throws Exception {
        if (!Files.exists(path)) Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    private static JsonObject read(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }
    private static double number(JsonObject o, String key, double fallback, double min, double max) {
        double n = o.has(key) ? o.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(n) || n < min || n > max) throw new IllegalArgumentException("Invalid " + key + ": " + n);
        return n;
    }
    private static int integer(JsonObject o, String key, int fallback, int min, int max) {
        double value = number(o, key, fallback, min, max);
        if (value != Math.floor(value)) throw new IllegalArgumentException("Expected integer: " + key);
        return (int)value;
    }
    private Rules() {}
}
