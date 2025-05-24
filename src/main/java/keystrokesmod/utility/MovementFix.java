package keystrokesmod.utility;

import keystrokesmod.event.*;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

public class MovementFix {
    private Minecraft mc;
    public static @Nullable Float rotationYaw = null;
    public static @Nullable Float rotationPitch = null;
    private boolean rotationed;

    public MovementFix(Minecraft mc) {
        this.mc = mc;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreUpdate(PreUpdateEvent event) {
        RotationEvent rotationEvent = new RotationEvent(RotationUtils.serverRotations[0], RotationUtils.serverRotations[1]);
        MinecraftForge.EVENT_BUS.post(rotationEvent);
        if (rotationEvent.isSet()) {
            rotationed = true;
            rotationYaw = rotationEvent.getYaw();
            rotationPitch = rotationEvent.getPitch();
        }
    }

    @SubscribeEvent
    public void onTick(GameTickEvent event) {
        rotationPitch = null;
        rotationYaw = null;
        rotationed = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onMoveInput(PrePlayerInputEvent event) {
        if (fixMovement() && rotationYaw != null && !Settings.strictMove.isToggled()) {
            final float forward = event.getForward();
            final float strafe = event.getStrafe();

            final double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(mc.thePlayer.rotationYaw, forward, strafe)));

            if (forward == 0 && strafe == 0) {
                return;
            }

            float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

            for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
                for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                    if (predictedStrafe == 0 && predictedForward == 0) continue;

                    final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(rotationYaw, predictedForward, predictedStrafe)));
                    final double difference = wrappedDifference(angle, predictedAngle);

                    if (difference < closestDifference) {
                        closestDifference = (float) difference;
                        closestForward = predictedForward;
                        closestStrafe = predictedStrafe;
                    }
                }
            }
            event.setForward(closestForward);
            event.setStrafe(closestStrafe);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJump(JumpEvent e) {
        if (fixMovement() && rotationYaw != null) {
            e.setYaw(rotationYaw);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onStrafe(StrafeEvent event) {
        if (fixMovement() && rotationYaw != null) {
            event.setYaw(rotationYaw);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (rotationed) {
            if (rotationYaw != null) {
                e.setYaw(rotationYaw);
            }
            if (rotationPitch != null) {
                e.setPitch(rotationPitch);
            }
        }
    }

    private boolean fixMovement() {
        return Settings.movementFix != null && Settings.movementFix.isToggled() && rotationed;
    }

    public double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }

    public double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }
}
