package com.net.levelup;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LevelUp.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientHud {
    private static final java.util.Map<String, String> CHINESE_BIOMES = loadBiomeNames();
    private static String[] lines = new String[0];
    private static int ticks;
    private static float mspt = Float.NaN;
    private static float tps = Float.NaN;
    private static long received;
    private static Object connection;
    private static Object subscriptionConnection;
    private static boolean subscribed;
    private static HudSettings.Mode lastMode;
    private static net.minecraft.core.BlockPos lastPos;
    private static String coloredDay = "", coloredClock = "", name = "", statsText = "TPS: --  MSPT: --";
    private static int fps;
    private ClientHud() { }

    private static java.util.Map<String, String> loadBiomeNames() {
        try (var stream = ClientHud.class.getResourceAsStream("/assets/levelup/biomes_zh_cn.json")) {
            if (stream == null) return java.util.Map.of();
            try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                var names = new java.util.HashMap<String, String>();
                com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().entrySet()
                    .forEach(entry -> names.put(entry.getKey(), entry.getValue().getAsString()));
                return java.util.Map.copyOf(names);
            }
        } catch (java.io.IOException | RuntimeException e) {
            LevelUp.LOGGER.warn("Cannot load Chinese biome names", e);
            return java.util.Map.of();
        }
    }

    static void stats(float serverTps, float value) {
        connection = Minecraft.getInstance().getConnection();
        mspt = value;
        tps = serverTps;
        received = System.nanoTime();
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        MinecraftForge.EVENT_BUS.addListener(ClientHud::tick);
        event.registerAboveAll("information", (gui, graphics, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || mc.options.hideGui
                || mc.options.renderDebug || HudSettings.MODE.get() == HudSettings.Mode.OFF) return;
            int x = HudSettings.X.get(), y = HudSettings.Y.get();
            for (String line : lines) {
                graphics.drawString(mc.font, line, x, y, 0xFFFFFF, true);
                y += mc.font.lineHeight + 2;
            }
        });
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            lines = new String[0];
            mspt = Float.NaN;
            connection = null;
            ticks = 0;
            subscriptionConnection = null;
            subscribed = false;
            lastMode = null;
            lastPos = null;
            return;
        }
        var mode = HudSettings.MODE.get();
        boolean visible = mode != HudSettings.Mode.OFF && !mc.options.hideGui && !mc.options.renderDebug;
        boolean wantStats = visible && mode == HudSettings.Mode.FULL;
        if (subscriptionConnection != mc.getConnection() || subscribed != wantStats) {
            Network.subscribe(mc.getConnection().getConnection(), wantStats);
            subscriptionConnection = mc.getConnection();
            subscribed = wantStats;
            mspt = Float.NaN;
            tps = Float.NaN;
        }
        if (!visible) { lastMode = null; return; }
        var pos = mc.player.blockPosition();
        boolean slowUpdate = ++ticks % 5 == 0 || lastMode != mode;
        if (slowUpdate) {
            long time = mc.level.getDayTime();
            long day = BalanceMath.day(time);
            int minutes = (int)((Math.floorMod(time, 24000L) * 60 / 1000 + 360) % 1440);
            String clock = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
            var biome = mc.level.getBiome(pos).unwrapKey();
            name = biome.map(key -> {
                var id = key.location();
                String translation = "biome." + id.getNamespace() + "." + id.getPath().replace('/', '.');
                String chinese = CHINESE_BIOMES.get(translation);
                if (chinese != null) return chinese;
                return I18n.exists(translation) ? I18n.get(translation) : id.toString();
            }).orElse("未知");
            fps = mc.getFps();
            coloredDay = ChatFormatting.GREEN + Long.toString(day) + ChatFormatting.RESET;
            coloredClock = ChatFormatting.GREEN + clock + ChatFormatting.RESET;
            boolean valid = connection == mc.getConnection() && Float.isFinite(mspt) && mspt >= 0
                && Float.isFinite(tps) && tps >= 0 && tps <= 20
                && System.nanoTime() - received < 5_000_000_000L;
            ChatFormatting tpsColor = tps >= 20 ? ChatFormatting.GREEN : ChatFormatting.RED;
            ChatFormatting msptColor = mspt <= 40 ? ChatFormatting.GREEN
                : mspt <= 45 ? ChatFormatting.YELLOW : mspt <= 50 ? ChatFormatting.GOLD : ChatFormatting.RED;
            statsText = valid ? String.format(Locale.ROOT, "TPS: %s%.1f%s  MSPT: %s%.1f%s",
                tpsColor, tps, ChatFormatting.RESET, msptColor, mspt, ChatFormatting.RESET) : "TPS: --  MSPT: --";
        }
        if (!slowUpdate && pos.equals(lastPos)) return;
        lastPos = pos;
        lastMode = mode;
        if (HudSettings.MODE.get() == HudSettings.Mode.SIMPLE)
        {
            //fps + " FPS "
            lines = new String[] {"FPS: "+fps +" "+coloredDay + "天 "+ coloredClock + " [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] " + name};
        }
        else
        {
            lines = new String[] {"FPS: " + fps, "天数: 第 " + coloredDay + " 天 时间: " + coloredClock,
                "坐标: X: "+ pos.getX() + " Y: "+ pos.getY() + " Z: "+ pos.getZ(),
                statsText, "群系: " + name};
        }
    }
}
