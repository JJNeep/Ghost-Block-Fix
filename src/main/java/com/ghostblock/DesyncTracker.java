package com.ghostblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class DesyncTracker {

    public enum State { NORMAL, DESYNCED, CONVERGING }

    private static final double POSITION_THRESHOLD = 0.1;
    // How many ticks of silence from the server before we assume we're off the ghost block
    private static final int NO_CORRECTION_RELEASE_TICKS = 10;
    private static final int CONVERGENCE_TICKS = 10;
    private static final int CORRECTION_COUNT_THRESHOLD = 3;
    private static final double SAME_SPOT_THRESHOLD = 0.5;

    public static State state = State.NORMAL;
    public static double serverX, serverY, serverZ;
    public static float serverYaw, serverPitch;
    public static boolean serverPosKnown = false;

    private static int convergenceTicks = 0;
    private static int correctionCount = 0;
    private static int ticksSinceLastCorrection = 0;
    private static double lastCorrectionX, lastCorrectionY, lastCorrectionZ;

    public static void reset() {
        state = State.NORMAL;
        serverPosKnown = false;
        convergenceTicks = 0;
        correctionCount = 0;
        ticksSinceLastCorrection = 0;
        lastCorrectionX = 0;
        lastCorrectionY = 0;
        lastCorrectionZ = 0;
    }

    public static void onServerCorrection(double x, double y, double z, float yaw, float pitch) {
        serverX = x; serverY = y; serverZ = z;
        serverYaw = yaw; serverPitch = pitch;
        serverPosKnown = true;
        convergenceTicks = 0;
        ticksSinceLastCorrection = 0; // reset silence counter on every correction

        double dx = Math.abs(x - lastCorrectionX);
        double dy = Math.abs(y - lastCorrectionY);
        double dz = Math.abs(z - lastCorrectionZ);

        if (dx < SAME_SPOT_THRESHOLD && dy < SAME_SPOT_THRESHOLD && dz < SAME_SPOT_THRESHOLD) {
            correctionCount++;
        } else {
            correctionCount = 1;
            if (state == State.DESYNCED) {
                // Correction at a new spot means we moved somewhere the server
                // disagrees with — re-anchor to new position but stay DESYNCED
                state = State.NORMAL;
            }
        }

        lastCorrectionX = x;
        lastCorrectionY = y;
        lastCorrectionZ = z;

        if (correctionCount >= CORRECTION_COUNT_THRESHOLD) {
            state = State.DESYNCED;
            GhostBlockMod.LOGGER.debug(
                "Ghost block detected ({} corrections) -> DESYNCED @ {},{},{}",
                correctionCount, x, y, z);
        }
    }

    public static void tick() {
        if (!serverPosKnown) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (state == State.DESYNCED) {
            ticksSinceLastCorrection++;

            // PRIMARY EXIT: server has gone quiet — it's happy with where we are,
            // meaning we've walked off the ghost block onto ground it accepts.
            // Release the Y freeze and go back to normal.
            if (ticksSinceLastCorrection >= NO_CORRECTION_RELEASE_TICKS) {
                state = State.CONVERGING;
                convergenceTicks = 0;
                GhostBlockMod.LOGGER.debug(
                    "No correction for {} ticks -> server happy -> CONVERGING",
                    ticksSinceLastCorrection);
                return;
            }

            // SECONDARY EXIT: natural Y convergence — client would land at serverY anyway
            double naturalY = player.getY() + player.getDeltaMovement().y;
            if (Math.abs(naturalY - serverY) < POSITION_THRESHOLD && player.onGround()) {
                state = State.CONVERGING;
                convergenceTicks = 0;
                GhostBlockMod.LOGGER.debug("Natural Y convergence -> CONVERGING");
                return;
            }

            // TERTIARY EXIT: jump — manual escape
            if (Minecraft.getInstance().options.keyJump.isDown()) {
                state = State.NORMAL;
                correctionCount = 0;
                ticksSinceLastCorrection = 0;
                GhostBlockMod.LOGGER.debug("Jump -> releasing freeze -> NORMAL");
                return;
            }

            // Still desynced — pin Y only, let X/Z move freely
            player.setPos(player.getX(), serverY, player.getZ());
            player.setDeltaMovement(
                player.getDeltaMovement().x,
                0.0,
                player.getDeltaMovement().z
            );
            player.setOnGround(true);

        } else if (state == State.CONVERGING) {
            double dx = player.getX() - serverX;
            double dy = player.getY() - serverY;
            double dz = player.getZ() - serverZ;
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

            if (dist > POSITION_THRESHOLD) {
                // Drifted again — back on a ghost block
                state = State.DESYNCED;
                convergenceTicks = 0;
                ticksSinceLastCorrection = 0;
                GhostBlockMod.LOGGER.debug("Drift -> DESYNCED");
            } else if (++convergenceTicks >= CONVERGENCE_TICKS) {
                state = State.NORMAL;
                serverPosKnown = false;
                correctionCount = 0;
                convergenceTicks = 0;
                ticksSinceLastCorrection = 0;
                GhostBlockMod.LOGGER.debug("Stable -> NORMAL");
            }
        }
    }

    public static boolean shouldLockOutgoing() { return state == State.DESYNCED; }
    public static boolean shouldPreserveRotation() { return state != State.NORMAL; }
}
