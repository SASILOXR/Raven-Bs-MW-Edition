package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class JumpReset extends Module {
  private SliderSetting chance;
  private ButtonSetting requireMouseDown;
  private ButtonSetting requireMovingForward;
  private ButtonSetting requireAim;
  private boolean jump;
  private boolean aiming;
  private boolean ignoreNext;
  private int LastHurtTime;
  private double lastFallDistance;
  private ButtonSetting disableInLiquid;

  public JumpReset() {
    super("Jump Reset", category.combat);
    this.registerSetting(chance = new SliderSetting("Chance", "%", 80, 0, 100, 1));
    this.registerSetting(requireMouseDown = new ButtonSetting("Require Mouse Down", true));
    this.registerSetting(requireMovingForward = new ButtonSetting("Require Moving Forward", true));
    this.registerSetting(requireAim = new ButtonSetting("Require Aim", true));
    this.registerSetting(disableInLiquid = new ButtonSetting("Disable In Liquid", true));
    this.closetModule = true;
  }

  public void onDisable() {
    jump = false;
  }

  @Override
  public String getInfo() {
    return String.valueOf(chance.getInput());
  }

  @SubscribeEvent
  public void onPreUpdate(PreUpdateEvent e) {
    int hurtTime = mc.thePlayer.hurtTime;
    boolean onGround = mc.thePlayer.onGround;
    if (onGround && lastFallDistance > 3 && !mc.thePlayer.capabilities.allowFlying)
      ignoreNext = true;
    if (hurtTime > LastHurtTime) {
      boolean mouseDown = Mouse.isButtonDown(0) || !requireMouseDown.isToggled();
      boolean aimingAt = aiming || !requireAim.isToggled();
      boolean forward =
          mc.gameSettings.keyBindForward.isKeyDown() || !requireMovingForward.isToggled();
      boolean liquidCheck = !(disableInLiquid.isToggled() && Utils.inLiquid());
      if (!ignoreNext
          && !mc.thePlayer.isBurning()
          && onGround
          && aimingAt
          && forward
          && mouseDown
          && Utils.randomizeDouble(0, 100) < chance.getInput()
          && liquidCheck
          && !hasBadEffect()) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), jump = true);
        KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
        if (Raven.debug) {
          Utils.sendModuleMessage(this, "&7jumping enabled");
        }
      }
      ignoreNext = false;
    }
    LastHurtTime = hurtTime;
    lastFallDistance = mc.thePlayer.fallDistance;
  }

  @SubscribeEvent
  public void onPostUpdate(PostMotionEvent e) {
    if (jump && !Utils.jumpDown()) {
      KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), jump = false);
    }
    if (Raven.debug) {
      Utils.sendModuleMessage(this, "&7jumping disabled");
    }
  }

  @SubscribeEvent
  public void onSendPacket(SendPacketEvent e) {
    if (e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook) {
      C03PacketPlayer.C05PacketPlayerLook event = (C05PacketPlayerLook) e.getPacket();
      checkAim(event.getYaw(), event.getPitch());
    } else if (e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
      C03PacketPlayer.C06PacketPlayerPosLook event = (C06PacketPlayerPosLook) e.getPacket();
      checkAim(event.getYaw(), event.getPitch());
    }
  }

  private boolean hasBadEffect() {
    for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
      int id = potionEffect.getPotionID();
      return id == Potion.jump.getId()
          || id == Potion.poison.getId()
          || id == Potion.wither.getId();
    }
    return false;
  }

  private void checkAim(float yaw, float pitch) {
    MovingObjectPosition result =
        RotationUtils.rayTrace(
            5,
            ((IAccessorMinecraft) mc).getTimer().renderPartialTicks,
            new float[] {yaw, pitch},
            null);
    aiming =
        result != null
            && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
            && result.entityHit instanceof EntityOtherPlayerMP;
  }
}
