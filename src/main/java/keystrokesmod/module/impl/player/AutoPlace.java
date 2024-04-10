package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AutoPlace extends Module {
    public static DescriptionSetting description;
    public static ButtonSetting holdRight;
    public static ButtonSetting b;
    public static SliderSetting frameDelay;
    private double lfd = 0.0D;
    private long l = 0L;
    private int f = 0;
    private MovingObjectPosition lm = null;
    private BlockPos lp = null;

    public AutoPlace() {
        super("AutoPlace", Module.category.player, 0);
        this.registerSetting(description = new DescriptionSetting("Best with safewalk."));
        this.registerSetting(frameDelay = new SliderSetting("Frame delay", 8.0D, 0.0D, 30.0D, 1.0D));
        this.registerSetting(holdRight = new ButtonSetting("Hold right", true));
    }

    public void guiUpdate() {
        if (this.lfd != frameDelay.getInput()) {
            this.rv();
        }

        this.lfd = frameDelay.getInput();
    }

    public void onDisable() {
        if (holdRight.isToggled()) {
            this.rd(4);
        }

        this.rv();
    }

    public void onUpdate() {
        if (holdRight.isToggled() && Mouse.isButtonDown(1) && !mc.thePlayer.capabilities.isFlying && !ModuleManager.fastPlace.isEnabled()) {
            ItemStack i = mc.thePlayer.getHeldItem();
            if (i == null || !(i.getItem() instanceof ItemBlock)) {
                return;
            }

            this.rd(mc.thePlayer.motionY > 0.0D ? 1 : 1000);
        }

    }

    @SubscribeEvent(
            priority = EventPriority.HIGHEST
    )
    public void bh(DrawBlockHighlightEvent ev) {
        if (Utils.nullCheck()) {
            if (mc.currentScreen == null && !mc.thePlayer.capabilities.isFlying) {
                ItemStack i = mc.thePlayer.getHeldItem();
                if (i != null && i.getItem() instanceof ItemBlock) {
                    MovingObjectPosition m = mc.objectMouseOver;
                    if (m != null && m.typeOfHit == MovingObjectType.BLOCK && m.sideHit != EnumFacing.UP && m.sideHit != EnumFacing.DOWN) {
                        if (this.lm != null && (double) this.f < frameDelay.getInput()) {
                            ++this.f;
                        } else {
                            this.lm = m;
                            BlockPos pos = m.getBlockPos();
                            if (this.lp == null || pos.getX() != this.lp.getX() || pos.getY() != this.lp.getY() || pos.getZ() != this.lp.getZ()) {
                                Block b = mc.theWorld.getBlockState(pos).getBlock();
                                if (b != null && b != Blocks.air && !(b instanceof BlockLiquid)) {
                                    if (!holdRight.isToggled() || Mouse.isButtonDown(1)) {
                                        long n = System.currentTimeMillis();
                                        if (n - this.l >= 25L) {
                                            this.l = n;
                                            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, i, pos, m.sideHit, m.hitVec)) {
                                                Reflection.setButton(1, true);
                                                mc.thePlayer.swingItem();
                                                mc.getItemRenderer().resetEquippedProgress();
                                                Reflection.setButton(1, false);
                                                this.lp = pos;
                                                this.f = 0;
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void rd(int i) {
        try {
            if (Reflection.rightClickDelayTimerField != null) {
                Reflection.rightClickDelayTimerField.set(mc, i);
            }
        } catch (IllegalAccessException | IndexOutOfBoundsException var3) {
        }
    }

    private void rv() {
        this.lp = null;
        this.lm = null;
        this.f = 0;
    }
}
