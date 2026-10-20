package com.net.levelup;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Client-only viewpoint; never inserted into the world or sent to the server. */
@Mod.EventBusSubscriber(modid = LevelUp.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FreeCamera {
    private static final KeyMapping TOGGLE = new KeyMapping("key.levelup.free_camera", GLFW.GLFW_KEY_F6, "key.categories.levelup");
    private static volatile RemotePlayer camera;
    private static LocalPlayer owner;
    private static Entity previousCamera;
    private static CameraType previousType;
    private static boolean previousCull;
    private static Vec3 motion = Vec3.ZERO;
    private static boolean sprinting;
    private static int nextRequest;
    private static int pendingRequest;
    private static LocalPlayer requestingPlayer;
    private static net.minecraft.client.multiplayer.ClientLevel requestingLevel;
    private static net.minecraft.network.Connection requestingConnection;
    private static long requestStarted;

    private FreeCamera() { }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
        MinecraftForge.EVENT_BUS.addListener(FreeCamera::tick);
        MinecraftForge.EVENT_BUS.addListener(FreeCamera::input);
    }

    public static boolean active() { return camera != null; }

    private static void cancelRequest() {
        pendingRequest = 0;
        requestingPlayer = null;
        requestingLevel = null;
        requestingConnection = null;
    }

    private static void request(Minecraft mc) {
        if (pendingRequest != 0) { cancelRequest(); return; }
        if (mc.player == null || mc.level == null || mc.getConnection() == null
                || !mc.player.isAlive() || mc.player.isSleeping() || mc.getCameraEntity() != mc.player) return;
        if (nextRequest == Integer.MAX_VALUE) nextRequest = 0;
        pendingRequest = ++nextRequest;
        requestingPlayer = mc.player;
        requestingLevel = mc.level;
        requestingConnection = mc.getConnection().getConnection();
        requestStarted = System.nanoTime();
        if (!Network.requestFreeCamera(requestingConnection, pendingRequest)) {
            cancelRequest();
            mc.player.displayClientMessage(Component.translatable("levelup.free_camera.unsupported"), true);
        }
    }

    static void permission(net.minecraft.network.Connection connection, int requestId, boolean allowed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.getConnection().getConnection() != connection) return;
        // Login/respawn/dimension snapshots invalidate outstanding requests. They
        // never enable the camera; every F6 activation requires a fresh decision.
        if (requestId == 0) { cancelRequest(); return; }
        if (requestId != pendingRequest || requestingConnection != connection
                || mc.player != requestingPlayer || mc.level != requestingLevel) return;
        boolean valid = System.nanoTime() - requestStarted < 5_000_000_000L;
        cancelRequest();
        if (!valid || mc.screen != null) return;
        if (allowed) enable(mc);
        else if (mc.player != null) mc.player.displayClientMessage(Component.translatable("levelup.free_camera.locked"), true);
    }

    public static void turn(double yaw, double pitch) {
        if (camera == null) return;
        camera.setYRot(camera.getYRot() + (float) yaw * 0.15F);
        camera.setXRot(Mth.clamp(camera.getXRot() + (float) pitch * 0.15F, -90F, 90F));
        camera.yHeadRot = camera.getYRot();
    }

    private static void enable(Minecraft mc) {
        if (mc.player == null || mc.level == null || !mc.player.isAlive() || mc.player.isSleeping()
                || mc.getCameraEntity() != mc.player) return;
        owner = mc.player;
        previousCamera = mc.getCameraEntity();
        previousType = mc.options.getCameraType();
        previousCull = mc.smartCull;
        camera = new RemotePlayer(mc.level, owner.getGameProfile()) {
            @Override public boolean isSpectator() { return true; }
            // Match LocalPlayer: mouse angles are sampled each frame, not interpolated
            // through RemotePlayer's independently updated head rotations.
            @Override public float getViewYRot(float partialTick) { return getYRot(); }
            @Override public float getViewXRot(float partialTick) { return getXRot(); }
        };
        camera.moveTo(owner.getX(), owner.getEyeY() - camera.getEyeHeight(), owner.getZ(), owner.getYRot(), owner.getXRot());
        snapshot();
        motion = Vec3.ZERO;
        sprinting = false;
        owner.setSprinting(false);
        mc.gameMode.stopDestroyBlock();
        if (owner.isUsingItem()) mc.gameMode.releaseUsingItem(owner);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.smartCull = false;
        mc.setCameraEntity(camera);
        mc.player.displayClientMessage(Component.translatable("levelup.free_camera.enabled"), true);
    }

    public static void disable() {
        cancelRequest();
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        // A respawn or dimension transition may have already installed a new camera.
        if (mc.getCameraEntity() == camera) {
            mc.setCameraEntity(previousCamera == owner ? mc.player : previousCamera);
        }
        FreeCameraRendering.returned((net.minecraft.client.multiplayer.ClientLevel) camera.level(), camera.getX(), camera.getZ());
        mc.options.setCameraType(previousType);
        mc.smartCull = previousCull;
        camera = null;
        owner = null;
        previousCamera = null;
        motion = Vec3.ZERO;
        sprinting = false;
        if (mc.player != null) mc.player.displayClientMessage(Component.translatable("levelup.free_camera.disabled"), true);
    }

    private static void snapshot() {
        camera.xo = camera.xOld = camera.getX();
        camera.yo = camera.yOld = camera.getY();
        camera.zo = camera.zOld = camera.getZ();
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        camera.yHeadRotO = camera.yHeadRot = camera.getYRot();
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        if (pendingRequest != 0 && (mc.player != requestingPlayer || mc.level != requestingLevel
                || mc.getConnection() == null || mc.getConnection().getConnection() != requestingConnection
                || mc.screen != null)) cancelRequest();
        if (pendingRequest != 0 && System.nanoTime() - requestStarted >= 5_000_000_000L) {
            cancelRequest();
            if (mc.player != null) mc.player.displayClientMessage(Component.translatable("levelup.free_camera.timeout"), true);
        }
        if (active() && (mc.player != owner || mc.level != camera.level() || !owner.isAlive()
                || owner.isSleeping() || mc.getCameraEntity() != camera)) disable();
        while (TOGGLE.consumeClick()) {
            if (mc.screen == null) {
                if (active()) disable(); else request(mc);
            }
        }
        if (!active()) return;
        snapshot();
        // Do not leave queued world actions to execute when returning to the body.
        for (KeyMapping key : new KeyMapping[] {mc.options.keyAttack, mc.options.keyUse, mc.options.keyPickItem, mc.options.keyDrop}) {
            key.setDown(false);
            while (key.consumeClick()) { }
        }
        if (mc.screen != null || !mc.isWindowActive() || mc.isPaused()) {
            motion = Vec3.ZERO;
            sprinting = false;
            return;
        }
        int forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        int strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        int up = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        if (mc.options.keySprint.isDown()) sprinting = true;
        else if (!mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) sprinting = false;
        boolean diagonal = forward != 0 && strafe != 0;
        // Keep velocity in camera-local axes, as in the reference, so rotating the
        // view steers immediately rather than blending an old world-space heading.
        motion = new Vec3(FreeCameraMotion.horizontal(motion.x, forward, diagonal),
                FreeCameraMotion.ramp(motion.y, up),
                FreeCameraMotion.horizontal(motion.z, strafe, diagonal));
        double forwardSpeed = motion.x * (sprinting ? 3 : 1);
        double yaw = Math.toRadians(camera.getYRot());
        Vec3 movement = new Vec3(motion.z * Math.cos(yaw) - forwardSpeed * Math.sin(yaw), motion.y,
                forwardSpeed * Math.cos(yaw) + motion.z * Math.sin(yaw)).scale(0.7);
        camera.setDeltaMovement(movement);
        camera.setPos(camera.position().add(movement));
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void input(MovementInputUpdateEvent event) {
        if (!active() || event.getEntity() != owner) return;
        var input = event.getInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }
}
