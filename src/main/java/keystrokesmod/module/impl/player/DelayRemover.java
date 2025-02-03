package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DelayRemover extends Module {
    public static ButtonSetting oldReg, removeJumpTicks;

    public DelayRemover() {
        super("Delay Remover", category.player, 0);
        this.registerSetting(oldReg = new ButtonSetting("1.7 hitreg", true));
        this.registerSetting(removeJumpTicks = new ButtonSetting("Remove jump ticks", false));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !mc.inGameHasFocus || !Utils.nullCheck()) {
            return;
        }
        if (oldReg.isToggled()) {
            ((IAccessorMinecraft) mc).setLeftClickCounter(0);
        }
        if (removeJumpTicks.isToggled()) {
            ((IAccessorEntityLivingBase) mc.thePlayer).setJumpTicks(0);
        }
    }
}
