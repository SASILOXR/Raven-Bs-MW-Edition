package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DelayRemover extends Module {
  public static ButtonSetting oldReg, removeJumpTicks;
  public static SliderSetting jumpDelay;
  public static ButtonSetting disableInWater;
  public static int countTicks = 0;

  public DelayRemover() {
    super("Delay Remover", category.player, 0);
    this.registerSetting(oldReg = new ButtonSetting("1.7 hitreg", true));
    this.registerSetting(removeJumpTicks = new ButtonSetting("Remove Jump Ticks", false));
    this.registerSetting(jumpDelay = new SliderSetting("Jump Delay", 0, 0, 10, 1));
    this.registerSetting(disableInWater = new ButtonSetting("Disable In Water", true));
    this.closetModule = true;
  }

  @Override
  public String getInfo() {
    return String.valueOf(jumpDelay.getInput());
  }

  @SubscribeEvent
  public void onTick(final TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !mc.inGameHasFocus || !Utils.nullCheck()) {
      return;
    }
    if (oldReg.isToggled()) {
      ((IAccessorMinecraft) mc).setLeftClickCounter(0);
    }
    if (disableInWater.isToggled() && Utils.inLiquid()) {
      return;
    }
    if (removeJumpTicks.isToggled()) {
      int jumpTick = (int) jumpDelay.getInput();
      int currentJumpTick = ((IAccessorEntityLivingBase) mc.thePlayer).getJumpTicks();
      if (currentJumpTick > jumpTick) {
        ((IAccessorEntityLivingBase) mc.thePlayer).setJumpTicks(jumpTick);
      }
    }
  }
}
