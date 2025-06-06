package keystrokesmod.module.impl.combat;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class JumpReset extends Module {
  private SliderSetting chance;
  private SliderSetting motion;
  private boolean jump;

  public JumpReset() {
    super("Jump Reset", category.combat);
    this.registerSetting(chance = new SliderSetting("Chance", "%", 80, 0, 100, 1));
    this.registerSetting(motion = new SliderSetting("Jump motion", 0.42, 0, 1, 0.01));
    this.closetModule = true;
  }

  public void onDisable() {
    jump = false;
  }

  @SubscribeEvent
  public void onPreMoveInput(PrePlayerInputEvent e) {
    if (Utils.nullCheck()) {
      if (chance.getInput() == 0) {
        return;
      }
      if (mc.thePlayer.maxHurtTime <= 0) {
        jump = false;
        return;
      }
      if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
        jump = true;
      }
      if (!jump || mc.thePlayer.hurtTime == 0) {
        jump = false;
        return;
      }
      if (chance.getInput() != 100.0D) {
        double ch = Math.random();
        if (ch >= chance.getInput() / 100.0D) {
          return;
        }
      }
      if (jump && mc.thePlayer.onGround) {
        e.setJump(true);
        jump = false;
      }
    }
  }

  @SubscribeEvent
  public void onJump(JumpEvent e) {
    if (!Utils.nullCheck() || !jump) {
      return;
    }
    if (motion.getInput() != 0.42) {
      e.setMotionY((float) motion.getInput());
    }
    jump = false;
  }
}
