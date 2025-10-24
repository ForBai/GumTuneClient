package rosegold.gumtuneclient.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import rosegold.gumtuneclient.GumTuneClient;
import rosegold.gumtuneclient.events.PlayerMoveEvent;

import static rosegold.gumtuneclient.GumTuneClient.mc;

public class RotationUtils {

    public static Rotation startRot;
    public static Rotation endRot;
    private static long startTime;
    private static long endTime;

    private static float serverPitch;
    private static float serverYaw;
    public static float currentFakeYaw;
    public static float currentFakePitch;
    public static boolean done = true;

    private enum RotationType {
        NORMAL,
        SERVER
    }

    private static RotationType rotationType;

    public static class Rotation {
        public float pitch;
        public float yaw;

        public Rotation(float pitch, float yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
        }

        public float getValue() {
            return Math.abs(this.yaw) + Math.abs(this.pitch);
        }

        public float getPitch() {
            return this.pitch;
        }

        public float getYaw() {
            return this.yaw;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        @Override
        public String toString() {
            return "pitch=" + pitch +
                    ", yaw=" + yaw;
        }
    }

    public static double wrapAngleTo180(double angle) {
        return angle - Math.floor(angle / 360 + 0.5) * 360;
    }

    public static float wrapAngleTo180(float angle) {
        return (float) (angle - Math.floor(angle / 360 + 0.5) * 360);
    }

    public static float fovToVec3(Vec3 vec) {
        double x = vec.xCoord - mc.thePlayer.posX;
        double z = vec.zCoord - mc.thePlayer.posZ;
        double yaw = Math.atan2(x, z) * 57.2957795;
        return (float) (yaw * -1.0);
    }

    public static Rotation getRotation(final Vec3 from, final Vec3 to) {
        double diffX = to.xCoord - from.xCoord;
        double diffY = to.yCoord - from.yCoord;
        double diffZ = to.zCoord - from.zCoord;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float pitch = (float) -Math.atan2(dist, diffY);
        float yaw = (float) Math.atan2(diffZ, diffX);
        pitch = (float) wrapAngleTo180((pitch * 180F / Math.PI + 90) * -1);
        yaw = (float) wrapAngleTo180((yaw * 180 / Math.PI) - 90);

        return new Rotation(pitch, yaw);
    }

    public static Rotation getRotation(Vec3 vec3) {
        return getRotation(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), vec3);
    }

    public static Rotation getRotation(BlockPos block) {
        return getRotation(new Vec3(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5));
    }

    public static Rotation getRotation(Entity entity) {
        return getRotation(new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ));
    }

    public static Rotation getRotation(Entity entity, Vec3 offset) {
        return getRotation(new Vec3(entity.posX + offset.xCoord, entity.posY + offset.yCoord, entity.posZ + offset.zCoord));
    }

    public static Rotation getNeededChange(Rotation startRot, Rotation endRot) {
        float yawDiff = wrapAngleTo180(endRot.yaw) - wrapAngleTo180(startRot.yaw);

        if (yawDiff <= -180) {
            yawDiff += 360;
        } else if (yawDiff > 180) {
            yawDiff -= 360;
        }

        return new Rotation(endRot.pitch - startRot.pitch, yawDiff);
    }

    public static Vec3 getVectorForRotation(final float pitch, final float yaw) {
        final float f2 = -MathHelper.cos(-pitch * 0.017453292f);
        return new Vec3(MathHelper.sin(-yaw * 0.017453292f - 3.1415927f) * f2, MathHelper.sin(-pitch * 0.017453292f), MathHelper.cos(-yaw * 0.017453292f - 3.1415927f) * f2);
    }

    public static Vec3 getLook(final Vec3 vec) {
        final double diffX = vec.xCoord - mc.thePlayer.posX;
        final double diffY = vec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double diffZ = vec.zCoord - mc.thePlayer.posZ;
        final double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        return getVectorForRotation((float) (-(MathHelper.atan2(diffY, dist) * 180.0 / 3.141592653589793)), (float) (MathHelper.atan2(diffZ, diffX) * 180.0 / 3.141592653589793 - 90.0));
    }

    public static Rotation getNeededChange(Rotation endRot) {
        return getNeededChange(new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw), endRot);
    }

    public static Rotation getServerNeededChange(Rotation endRotation) {
        return endRot == null ? getNeededChange(endRotation) : getNeededChange(endRot, endRotation);
    }

    private static float interpolate(float start, float end) {
        return (end - start) * easeOutCubic((float) (System.currentTimeMillis() - startTime) / (endTime - startTime)) + start;
    }

    public static float easeOutCubic(double number) {
        return (float) Math.max(0, Math.min(1, 1 - Math.pow(1 - number, 3)));
    }

    public static void smoothLookRelative(Rotation rotation, long time) {
        smoothLookRelative(rotation, time, false);
    }

    public static void smoothLookRelative(Rotation rotation, long time, boolean yawOnly) {
        rotationType = RotationType.NORMAL;
        done = false;

        startRot = new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw);

        if (yawOnly) {
            endRot = new Rotation(rotation.pitch, startRot.yaw + rotation.yaw);
        } else {
            endRot = new Rotation(startRot.pitch + rotation.pitch, startRot.yaw + rotation.yaw);
        }

        if (GumTuneClient.debug) ModUtils.sendMessage(endRot);

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
    }

    public static void smoothLook(Rotation rotation, long time) {
        rotationType = RotationType.NORMAL;
        done = false;
        startRot = new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw);

        Rotation neededChange = getNeededChange(startRot, rotation);

        endRot = new Rotation(startRot.pitch + neededChange.pitch, startRot.yaw + neededChange.yaw);

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
    }

//    public static void smoothLook(Rotation rotation, long time,float random) {
//        rotationType = RotationType.NORMAL;
//        done = false;
//        startRot = new Rotation(mc.thePlayer.rotationPitch, mc.thePlayer.rotationYaw);
//
//        Rotation neededChange = getNeededChange(startRot, rotation);
//
//        endRot = new Rotation(startRot.pitch + neededChange.pitch, startRot.yaw + neededChange.yaw);
//
//        startTime = System.currentTimeMillis();
//        endTime = System.currentTimeMillis() + time;
//    }

    public static void smartSmoothLook(Rotation rotation, int msPer180) {
        float rotationDifference = wrapAngleTo180(Math.max(
                Math.abs(rotation.pitch - mc.thePlayer.rotationPitch),
                Math.abs(rotation.yaw - mc.thePlayer.rotationYaw)
        ));
        smoothLook(rotation, (int) (rotationDifference / 180 * msPer180));
    }

    public static void serverSmoothLookRelative(Rotation rotation, long time) {
        rotationType = RotationType.SERVER;
        done = false;

        if (currentFakePitch == 0) currentFakePitch = mc.thePlayer.rotationPitch;
        if (currentFakeYaw == 0) currentFakeYaw = mc.thePlayer.rotationYaw;

        startRot = new Rotation(currentFakePitch, currentFakeYaw);

        endRot = new Rotation(startRot.pitch + rotation.pitch, startRot.yaw + rotation.yaw);

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
    }

    public static void serverSmoothLook(Rotation rotation, long time) {
        rotationType = RotationType.SERVER;
        done = false;

        if (currentFakePitch == 0) currentFakePitch = mc.thePlayer.rotationPitch;
        if (currentFakeYaw == 0) currentFakeYaw = mc.thePlayer.rotationYaw;

        startRot = new Rotation(currentFakePitch, currentFakeYaw);

        Rotation neededChange = getNeededChange(startRot, rotation);

        endRot = new Rotation(startRot.pitch + neededChange.pitch, startRot.yaw + neededChange.yaw);

        startTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis() + time;
    }

    /**
     * Update loop for the server-rotation path that interpolates the player's view from {@code startRot}
     * to {@code endRot} and then resets the smoothing state when finished.
     * <p>
     * While animating, {@link #currentFakeYaw} and {@link #currentFakePitch} are kept in sync with the
     * values written to {@code mc.thePlayer} so subsequent server-smoothing calls can continue smoothly.
     * On completion (when current time exceeds {@link #endTime}), we snap to {@code endRot} and call {@link #reset()}.
     */
    public static void updateServerLookResetting() {
        if (System.currentTimeMillis() <= endTime) {
            // Interpolate pitch/yaw towards target using the configured easing.
            mc.thePlayer.rotationYaw = interpolate(startRot.getYaw(), endRot.getYaw());
            mc.thePlayer.rotationPitch = interpolate(startRot.getPitch(), endRot.getPitch());

            // Maintain the fake rotation cache used by server smoothing.
            currentFakeYaw = mc.thePlayer.rotationYaw;
            currentFakePitch = mc.thePlayer.rotationPitch;
        } else {
            if (!done) {
                // Snap to the final rotation once the animation window has elapsed.
                mc.thePlayer.rotationYaw = endRot.getYaw();
                mc.thePlayer.rotationPitch = endRot.getPitch();

                currentFakeYaw = mc.thePlayer.rotationYaw;
                currentFakePitch = mc.thePlayer.rotationPitch;

                // Important difference vs. updateServerLook(): we reset() here.
                reset();
            }
        }
    }

    /**
     * Update loop for the server-rotation path that interpolates from {@code startRot} to {@code endRot}
     * but does NOT call {@link #reset()} after finishing. This allows callers to decide when to clear state.
     * <p>
     * The fake rotation cache ({@link #currentFakeYaw}, {@link #currentFakePitch}) is kept in sync while animating
     * and after snapping to {@code endRot}.
     */
    public static void updateServerLook() {
        if (System.currentTimeMillis() <= endTime) {
            // Interpolate pitch/yaw towards target using the configured easing.
            mc.thePlayer.rotationYaw = interpolate(startRot.getYaw(), endRot.getYaw());
            mc.thePlayer.rotationPitch = interpolate(startRot.getPitch(), endRot.getPitch());

            // Maintain the fake rotation cache used by server smoothing.
            currentFakeYaw = mc.thePlayer.rotationYaw;
            currentFakePitch = mc.thePlayer.rotationPitch;
        } else {
            if (!done) {
                // Snap to the final rotation once the animation window has elapsed.
                mc.thePlayer.rotationYaw = endRot.getYaw();
                mc.thePlayer.rotationPitch = endRot.getPitch();

                currentFakeYaw = mc.thePlayer.rotationYaw;
                currentFakePitch = mc.thePlayer.rotationPitch;
            }
        }
    }

    /**
     * Immediately set the player's view to the provided rotation without any interpolation.
     *
     * @param rotation absolute rotation (pitch, yaw) to apply to the player
     */
    public static void look(Rotation rotation) {
        mc.thePlayer.rotationPitch = rotation.pitch;
        mc.thePlayer.rotationYaw = rotation.yaw;
    }

    /**
     * Reset/clear any active rotation animation state.
     * Sets {@link #done} to true, clears start/end rotations and timing, and zeros the fake cache
     * ({@link #currentFakeYaw}, {@link #currentFakePitch}).
     */
    public static void reset() {
        done = true;
        startRot = null;
        endRot = null;
        startTime = 0;
        endTime = 0;
        currentFakeYaw = 0;
        currentFakePitch = 0;
    }

    /**
     * Render-phase interpolation for the client-side "NORMAL" rotation mode.
     *
     * <p>When {@link #rotationType} is {@code NORMAL}, this event advances the smooth rotation animation
     * between {@link #startRot} and {@link #endRot} based on the easing function. Once the time window
     * has elapsed, it snaps to {@code endRot} and calls {@link #reset()}.</p>
     *
     * @param event RenderWorldLastEvent hook provided by Forge
     */
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (rotationType != RotationType.NORMAL) return;
        if (System.currentTimeMillis() <= endTime) {
            // Apply eased interpolation in render stage to keep visuals smooth.
            mc.thePlayer.rotationPitch = interpolate(startRot.pitch, endRot.pitch);
            mc.thePlayer.rotationYaw = interpolate(startRot.yaw, endRot.yaw);
        } else {
            if (!done) {
                // Finish the animation and clear state.
                mc.thePlayer.rotationYaw = endRot.yaw;
                mc.thePlayer.rotationPitch = endRot.pitch;

                reset();
            }
        }
    }

    /**
     * Capture the server-synchronized rotation values at the beginning of the player's movement update.
     *
     * <p>Priority {@link EventPriority#HIGHEST} ensures we observe the original values before other handlers
     * may modify them for rendering or client-only effects.</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUpdatePre(PlayerMoveEvent.Pre pre) {
        serverPitch = mc.thePlayer.rotationPitch;
        serverYaw = mc.thePlayer.rotationYaw;
    }

    /**
     * Restore the server-synchronized rotation values after the player's movement update.
     *
     * <p>Priority {@link EventPriority#LOWEST} ensures this restoration runs after potential temporary
     * client-side adjustments so the server-facing state remains consistent.</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatePost(PlayerMoveEvent.Post post) {
        mc.thePlayer.rotationPitch = serverPitch;
        mc.thePlayer.rotationYaw = serverYaw;
    }
}