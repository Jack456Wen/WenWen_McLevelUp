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
    public record Day(long day, double probability, int maxOccurrences, String title, String announcement,
                      double health, double attack, double speed, double armor, String loot, double experienceMultiplier,
                      List<AttackEffects.Entry> attackEffects, List<AttackEffects.Entry> playerEffects, EventBgm bgm,
                      net.minecraft.world.BossEvent.BossBarColor bossBarColor, boolean mobParticles,
                      List<AttackEffects.Entry> mobEffects) {
        // Stable across reordering and changes to probability/effects; these three fields identify an event.
        String historyKey() {
            return UUID.nameUUIDFromBytes((day + "\u0000" + title + "\u0000" + loot)
                .getBytes(StandardCharsets.UTF_8)).toString();
        }
    }
    public record Entry(Item item, int count, double weight, double extraWeight) {}
    public record Snapshot(List<Day> days, Map<String, List<Entry>> tables) {}
    public static volatile Snapshot current = new Snapshot(List.of(), Map.of());
    public static Path directory() { return FMLPaths.CONFIGDIR.get().resolve(LevelUp.ID); }
    public static void reload() throws Exception {
        Path dir = directory();
        Files.createDirectories(dir);
        createDefault(dir.resolve("days.json"), """
            {"days":[{"day":7,"probability":0.15,"maxOccurrences":-1,"title":"文文","bossBarColor":"red","mobParticles":true,"announcement":"文本文本！","healthMultiplier":3,"attackMultiplier":2,"speedMultiplier":1.3,"armor":8,"lootTable":"wenwen","experienceMultiplier":1,"attackEffects":[],"playerEffects":[],"mobEffects":[],"bgm":"","bgmVolume":1,"bgmDurationSeconds":180}]}
            """);
        createDefault(dir.resolve("loot.json"), """
            {"tables":{
              "dimension":[
                {"item":"minecraft:iron_ingot","count":1,"weight":100,"extraWeight":0},
                {"item":"minecraft:gold_ingot","count":1,"weight":30,"extraWeight":60},
                {"item":"minecraft:diamond","count":1,"weight":3,"extraWeight":6},
                {"item":"minecraft:netherite_scrap","count":1,"weight":1,"extraWeight":2}],
              "wenwen":[
                {"item":"minecraft:gold_ingot","count":1,"weight":50,"extraWeight":0},
                {"item":"minecraft:diamond","count":1,"weight":5,"extraWeight":10}]
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
                double extraWeight = extraWeight(e, weight);
                // 轮空条目：item 为 null，抽中它就表示这次不掉额外物品。
                if (blank(e)) {
                    entries.add(new Entry(null, count, weight, extraWeight));
                    continue;
                }
                if (!e.has("item")) throw new IllegalArgumentException("Missing item in loot table: " + table.getKey());
                ResourceLocation id = ResourceLocation.tryParse(e.get("item").getAsString());
                if (id == null || !ForgeRegistries.ITEMS.containsKey(id))
                    throw new IllegalArgumentException("Unknown item: " + e.get("item"));
                entries.add(new Entry(ForgeRegistries.ITEMS.getValue(id), count, weight, extraWeight));
            }
            if (entries.isEmpty()) throw new IllegalArgumentException("Empty loot table: " + table.getKey());
            tables.put(table.getKey(), List.copyOf(entries));
        }
        if (!tables.containsKey("dimension")) throw new IllegalArgumentException("Missing dimension loot table");
        for (JsonElement element : read(dir.resolve("days.json")).getAsJsonArray("days")) {
            JsonObject d = element.getAsJsonObject();
            String table = d.get("lootTable").getAsString();
            if (!tables.containsKey(table)) throw new IllegalArgumentException("Missing loot table: " + table);
            days.add(new Day(integer(d,"day",1,1,Integer.MAX_VALUE), probability(d), maxOccurrences(d),
                d.get("title").getAsString(), d.get("announcement").getAsString(),
                multiplier(d,"healthMultiplier"), multiplier(d,"attackMultiplier"),
                multiplier(d,"speedMultiplier"), number(d,"armor",0,0,1000), table,
                number(d,"experienceMultiplier",1,0,100), AttackEffects.parse(d), PlayerEffects.parse(d), EventBgm.parse(d),
                bossBarColor(d), mobParticles(d), MobEffects.parse(d)));
        }
        current = new Snapshot(List.copyOf(days), Map.copyOf(tables));
    }
    static int maxOccurrences(JsonObject day) {
        if (!day.has("maxOccurrences")) return -1;
        JsonElement value = day.get("maxOccurrences");
        if (!value.isJsonPrimitive()) throw new IllegalArgumentException("maxOccurrences must be an integer");
        if (!value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("maxOccurrences must be an integer");
        try {
            var count = value.getAsBigDecimal().toBigIntegerExact();
            return count.signum() < 0 ? -1 : count.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("maxOccurrences must be an integer no greater than 2147483647", e);
        }
    }
    static double multiplier(JsonObject day, String key) {
        double value = number(day, key, 1, 0, 100);
        if (value <= 0) throw new IllegalArgumentException(key + " must be greater than 0");
        return value;
    }
    static net.minecraft.world.BossEvent.BossBarColor bossBarColor(JsonObject day) {
        if (!day.has("bossBarColor")) return net.minecraft.world.BossEvent.BossBarColor.RED;
        JsonElement value = day.get("bossBarColor");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("bossBarColor must be a color name");
        try {
            return net.minecraft.world.BossEvent.BossBarColor.valueOf(value.getAsString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("bossBarColor must be pink, blue, red, green, yellow, purple or white", e);
        }
    }
    static boolean mobParticles(JsonObject day) {
        if (!day.has("mobParticles")) return true;
        JsonElement value = day.get("mobParticles");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("mobParticles must be true or false");
        return value.getAsBoolean();
    }
    static double probability(JsonObject day) {
        if (day.has("repeatEvery"))
            throw new IllegalArgumentException("repeatEvery is no longer supported; replace it with probability (0 to 1)");
        if (!day.has("probability")) throw new IllegalArgumentException("Missing probability (0 to 1)");
        return number(day, "probability", 0, 0, 1);
    }
    public static ItemStack roll(String table, double progress, RandomSource random) {
        List<Entry> entries = current.tables.get(table);
        if (entries == null) return ItemStack.EMPTY;
        Entry entry = pick(entries, progress, random.nextDouble());
        // item 为 null 的轮空条目，或没有可抽取的条目时，都不产生额外掉落。
        if (entry == null || entry.item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(entry.item, entry.count);
        // 附魔书条目自动带一条宝藏附魔：附魔台刷不到的那些（经验修补、冰霜行者、灵魂疾行、迅捷潜行）。
        if (stack.is(Items.ENCHANTED_BOOK)) TreasureBook.apply(stack, random);
        return stack;
    }
    /**
     * 按实际权重挑出条目，{@code random} 取 [0,1)；没有可抽取的条目（总权重为 0）返回 null。
     * 抽奖与物品堆构建拆开，方便在没有 Minecraft 启动环境时测权重。
     */
    static Entry pick(List<Entry> entries, double progress, double random) {
        double total = 0;
        for (Entry e : entries) total += weight(e, progress);
        if (total <= 0) return null;
        double value = random * total;
        for (Entry e : entries) {
            value -= weight(e, progress);
            if (value < 0) return e;
        }
        return null;
    }
    /** 轮空条目：显式 {@code "blank": true}，或把 item 写成 {@code minecraft:air}（原版里空物品堆就是 air）。 */
    private static boolean blank(JsonObject e) {
        if (e.has("blank")) return e.get("blank").getAsBoolean();
        return e.has("item") && "minecraft:air".equals(e.get("item").getAsString());
    }
    static double extraWeight(JsonObject e, double weight) {
        if (e.has("rarity") || e.has("minStrength"))
            throw new IllegalArgumentException("rarity/minStrength are no longer supported; replace them with extraWeight");
        return number(e, "extraWeight", blank(e) ? 0 : weight * 2, 0, 2e9);
    }
    private static double weight(Entry e, double progress) { return BalanceMath.weight(e.weight, e.extraWeight, progress); }
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
