package keystrokesmod.utility;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.render.HUD;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.item.ItemFireball;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.Iterator;
import java.util.Map;

public class ModHelper {
    private final Minecraft mc;
    public static int inAirTicks;
    public static int groundTicks;
    private int unTargetTicks;

    public ModHelper(Minecraft mc) {
        this.mc = mc;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (inAirTicks <= 20) {
            inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;
        } else {
            inAirTicks = 19;
        }
        groundTicks = !mc.thePlayer.onGround ? 0 : ++groundTicks;

        int simpleY = (int) Math.round((e.posY % 1) * 10000);

        // 7 tick needs to always finish the motion or itll lag back
        if (!ModuleManager.bHop.isEnabled() && ModuleManager.bHop.mode.getInput() == 3 && ModuleManager.bHop.didMove) {
            if (mc.thePlayer.hurtTime == 0) {
                switch (simpleY) {
                    case 4200:
                        mc.thePlayer.motionY = 0.39;
                        break;
                    case 1138:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                        ModuleManager.bHop.lowhop = false;
                        ModuleManager.bHop.didMove = false;
                        break;
                }
            }
        }
        if (ModuleManager.bHop.setRotation) {
            if (KillAura.target == null && !ModuleManager.scaffold.isEnabled) {
                float yaw = mc.thePlayer.rotationYaw - 55;
                e.setYaw(yaw);
            }
            if (mc.thePlayer.onGround) {
                ModuleManager.bHop.setRotation = false;
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (ModuleManager.killAura.stoppedTargeting) {
            if (++unTargetTicks >= 2) {
                unTargetTicks = 0;
                ModuleManager.killAura.stoppedTargeting = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !ModuleManager.scaffold.highlightBlocks.isToggled() || ModuleManager.scaffold.highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }
}
