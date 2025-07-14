package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class DelayVelocity extends Module {

  private SliderSetting delayMs;
  private ButtonSetting requireMouseDown;
  private ButtonSetting onlyWeapon;
  private ButtonSetting lookAtPlayer;
  private ButtonSetting onlyAir;
  private ButtonSetting disableInLiquid;
  private ButtonSetting disableInInventroy;
  private SliderSetting chance;

  private List<Map<String, Object>> packets = new ArrayList<>();
  private boolean delaying, conditionals, aiming, teleported;

  public DelayVelocity() {
    super("DelayVelocity", category.combat);
    this.registerSetting(chance = new SliderSetting("Chance", "%", 80, 0, 100, 1));
    this.registerSetting(delayMs = new SliderSetting("Delay MS", "ms", 200, 0, 1000, 10));
    this.registerSetting(requireMouseDown = new ButtonSetting("Require Mouse Down", false));
    this.registerSetting(onlyWeapon = new ButtonSetting("Only Weapon", false));
    this.registerSetting(lookAtPlayer = new ButtonSetting("Look At Player", false));
    this.registerSetting(onlyAir = new ButtonSetting("Only Air", false));
    this.registerSetting(disableInLiquid = new ButtonSetting("Disable In Liquid", true));
    this.registerSetting(disableInInventroy = new ButtonSetting("Disable In Inventory", true));
  }

  @Override
  public void onEnable() {
    packets.clear();
    delaying = conditionals = aiming = teleported = false;
  }

  @Override
  public void onDisable() {
    flushAll();
  }

  @Override
  public String getInfo() {
    return String.valueOf(delayMs.getInput()) + "MS";
  }

  @SubscribeEvent
  public void onReceivePacket(ReceivePacketEvent e) {
    if (e.getPacket() instanceof S08PacketPlayerPosLook) {
      teleported = true;
    } else if (e.getPacket() instanceof S12PacketEntityVelocity) {
      teleported = false;
      S12PacketEntityVelocity packet = (S12PacketEntityVelocity) e.getPacket();
      if (packet.getEntityID() == mc.thePlayer.getEntityId() && conditionals) {
        delaying = true;
        if (Raven.debug) {
          Utils.sendMessage("delaying velocity packet");
        }
      }
    }
    if (!delaying) return;
    Map<String, Object> entry = new HashMap<>();
    entry.put("packet", e.getPacket());
    entry.put("time", System.currentTimeMillis());
    synchronized (packets) {
      packets.add(entry);
    }
    e.setCanceled(true);
  }

  @SubscribeEvent
  public void onPostMotion(PostMotionEvent e) {
    conditionals = conditionals();
    if (packets.isEmpty()) return;

    if (!conditionals || !containsVelocity() || teleported) {
      flushAll();
    }

    long now = System.currentTimeMillis();
    long delay = (long) delayMs.getInput();

    while (!packets.isEmpty()) {
      long timeReceive = (long) packets.get(0).get("time");
      if (now - timeReceive >= delay) {
        flushOne();
      } else {
        break;
      }
    }
  }

  @SubscribeEvent
  public void onSentPacket(SendPacketEvent e) {
    if (e.getPacket() instanceof C03PacketPlayer) {
      C03PacketPlayer c03 = (C03PacketPlayer) e.getPacket();
      if (c03 instanceof C05PacketPlayerLook || c03 instanceof C06PacketPlayerPosLook) {
        Object[] hit = Utils.raycastEntity(5, c03.getYaw(), c03.getPitch());
        aiming = hit != null && (Entity) hit[0] instanceof EntityPlayer;
      }
    }
  }

  private void flushOne() {
    synchronized (packets) {
      Map<String, Object> entry = packets.remove(0);
      PacketUtils.receivePacketNoEvent((Packet) entry.get("packet"));
    }
    if (Raven.debug) {
      Utils.sendMessage("relase velocity packet");
    }
  }

  private void flushAll() {
    while (!packets.isEmpty()) {
      flushOne();
    }
    delaying = false;
  }

  private boolean conditionals() {
    if (disableInInventroy.isToggled() && mc.currentScreen != null) return false;
    if (onlyWeapon.isToggled() && !Utils.holdingWeapon()) return false;
    if (requireMouseDown.isToggled() && !Mouse.isButtonDown(0)) return false;
    if (lookAtPlayer.isToggled() && !aiming) return false;
    if (mc.thePlayer.isCollidedHorizontally) return false;
    if (onlyAir.isToggled() && mc.thePlayer.onGround) return false;
    if (mc.thePlayer.capabilities.isFlying) return false;
    if (disableInLiquid.isToggled() && Utils.inLiquid()) return false;
    if (Utils.randomizeInt(0, 100) > chance.getInput()) return false;
    return true;
  }

  private boolean containsVelocity() {
    synchronized (packets) {
      for (Map<String, Object> entry : packets) {
        Packet packet = (Packet) entry.get("packet");
        if (packet instanceof S12PacketEntityVelocity
            && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId())
          return true;
      }
    }
    return false;
  }
}
