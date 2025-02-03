package keystrokesmod.module.impl.movement;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class NoSlow extends Module {
    public static SliderSetting mode;
    public static SliderSetting slowed;
    public static ButtonSetting disableBow;
    public static ButtonSetting disablePotions;
    public static ButtonSetting swordOnly;
    public static ButtonSetting vanillaSword;

    private String[] modes = new String[] { "Vanilla", "Pre", "Post", "Alpha", "Float" };

    private boolean postPlace;
    private boolean canFloat;
    private boolean reSendConsume;
    public boolean noSlowing;

    public NoSlow() {
        super("NoSlow", category.movement, 0);
        this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(slowed = new SliderSetting("Slow %", 80.0D, 0.0D, 80.0D, 1.0D));
        this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
        this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
        this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));
        this.registerSetting(vanillaSword = new ButtonSetting("Vanilla sword", false));
    }

    @Override
    public void onDisable() {
        resetFloat();
        noSlowing = false;
    }

    public void onUpdate() {
        if (ModuleManager.bedAura.stopAutoblock) {
            return;
        }
        postPlace = false;
        if (vanillaSword.isToggled() && Utils.holdingSword()) {
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            return;
        }
        switch ((int) mode.getInput()) {
            case 1:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
                break;
            case 2:
                postPlace = true;
                break;
            case 3:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                }
                break;
            case 4:
                if (reSendConsume) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                        break;
                    }
                    if (!mc.thePlayer.onGround) {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                        canFloat = true;
                        reSendConsume = false;
                    }
                }
                break;
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (postPlace && mode.getInput() == 2) {
            if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
            postPlace = false;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (ModuleManager.bedAura.stopAutoblock || mode.getInput() != 4) {
            resetFloat();
            return;
        }
        postPlace = false;
        if (!Mouse.isButtonDown(1)) {
            resetFloat();
            noSlowing = false;
            return;
        }
        if (vanillaSword.isToggled() && Utils.holdingSword()) {
            resetFloat();
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            resetFloat();
            return;
        }
        if ((canFloat && canFloat() && mc.thePlayer.onGround)) {
            e.setPosY(e.getPosY() + 1E-12);
            noSlowing = true;
        }
    }

    @SubscribeEvent
    public void onPacketSend(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && mode.getInput() == 4 && getSlowed() != 0.2f && holdingUsable(((C08PacketPlayerBlockPlacement) e.getPacket()).getStack()) && !BlockUtils.isInteractable(mc.objectMouseOver) && Utils.holdingEdible(((C08PacketPlayerBlockPlacement) e.getPacket()).getStack())) {
            if (((C08PacketPlayerBlockPlacement) e.getPacket()).getStack().getItem() instanceof ItemFood && mc.thePlayer.capabilities.isCreativeMode) {
                return;
            }
            if (!mc.thePlayer.onGround) {
                canFloat = true;
            }
            else {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }
                reSendConsume = true;
                canFloat = false;
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (reSendConsume) {
            e.setSprint(false);
        }
    }

    public static float getSlowed() {
        if (mc.thePlayer.getHeldItem() == null || ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled()) {
            return 0.2f;
        }
        else {
            if (swordOnly.isToggled() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return 0.2f;
            }
            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow && disableBow.isToggled()) {
                return 0.2f;
            }
            else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()) && disablePotions.isToggled()) {
                return 0.2f;
            }
        }
        float val = (100.0F - (float) slowed.getInput()) / 100.0F;
        return val;
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private void resetFloat() {
        reSendConsume = false;
        canFloat = false;
    }

    private boolean holdingUsable(ItemStack itemStack) {
        Item heldItem = itemStack.getItem();
        if (heldItem instanceof ItemFood || (heldItem instanceof ItemBow && Utils.hasArrows(itemStack)) || (heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage())) || (heldItem instanceof ItemSword && !vanillaSword.isToggled())) {
            return true;
        }
        return false;
    }

    private boolean canFloat() {
        if (mc.thePlayer.isOnLadder() || mc.thePlayer.isInLava() || mc.thePlayer.isInWater()) {
            return false;
        }
        return true;
    }
}