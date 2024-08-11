package keystrokesmod.module.impl.movement;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class InvMove extends Module {
    private SliderSetting motion;
    private ButtonSetting modifyMotionPost;
    private ButtonSetting slowWhenNecessary;
    private ButtonSetting allowJumping;
    public ButtonSetting invManagerOnly;
    private ButtonSetting rotateWithArrows;
    public int ticks;
    public boolean setMotion;

    public InvMove() {
        super("InvMove", Module.category.movement);
        this.registerSetting(motion = new SliderSetting("Motion", 1, 0.05, 1, 0.01, "x"));
        this.registerSetting(modifyMotionPost = new ButtonSetting("Modify motion after click", false));
        this.registerSetting(slowWhenNecessary = new ButtonSetting("Slow motion when necessary", false));
        this.registerSetting(allowJumping = new ButtonSetting("Allow jumping", true));
        this.registerSetting(invManagerOnly = new ButtonSetting("Only with inventory manager", false));
        this.registerSetting(rotateWithArrows = new ButtonSetting("Rotate with arrow keys", false));
    }

    public void onDisable() {
        reset();
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
    }

    public void onUpdate() {
        if (invManagerOnly.isToggled() && !ModuleManager.invManager.isEnabled()) {
            return;
        }
        if (mc.currentScreen != null) {
            if (mc.currentScreen instanceof GuiChat) {
                reset();
                return;
            }

            if (!(mc.currentScreen instanceof ClickGui)) {
                if (setMotion && !slowWhenNecessary.isToggled()) {
                    if (++ticks == 10) {
                        ticks = 0;
                        setMotion = false;
                    }
                }

                if (setMotion && motion.getInput() != 1) {
                    Utils.setSpeed(Utils.getHorizontalSpeed() * (slowWhenNecessary.isToggled() ? 0.65 : motion.getInput()));
                }
            }
            else {
                reset();
            }

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Utils.jumpDown());
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
            if (rotateWithArrows.isToggled()) {
                if (Keyboard.isKeyDown(208) && mc.thePlayer.rotationPitch < 90.0F) {
                    mc.thePlayer.rotationPitch += 6.0F;
                }
                if (Keyboard.isKeyDown(200) && mc.thePlayer.rotationPitch > -90.0F) {
                    mc.thePlayer.rotationPitch -= 6.0F;
                }
                if (Keyboard.isKeyDown(205)) {
                    mc.thePlayer.rotationYaw += 6.0F;
                }
                if (Keyboard.isKeyDown(203)) {
                    mc.thePlayer.rotationYaw -= 6.0F;
                }
            }
        }
        else {
            reset();
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (!allowJumping.isToggled() && mc.currentScreen != null && !(mc.currentScreen instanceof ClickGui)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C0EPacketClickWindow && (modifyMotionPost.isToggled() || slowWhenNecessary.isToggled())) {
            setMotion = true;
            ticks = 0;
        }
    }

    private void reset() {
        ticks = 0;
        setMotion = false;
    }
}
