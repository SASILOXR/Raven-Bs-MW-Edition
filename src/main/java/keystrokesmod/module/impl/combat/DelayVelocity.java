package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DelayVelocity extends Module {

  private SliderSetting delayMs;
  private ButtonSetting requireMouseDown;
  private ButtonSetting onlyWeapon;

  private List<Map<String, Object>> packets = new ArrayList<>();

  public DelayVelocity() {
    super("DelayVelocity", category.combat);
    this.registerSetting(delayMs = new SliderSetting("Delay MS", "ms", 400, 0, 1000, 10));
    this.registerSetting(requireMouseDown = new ButtonSetting("require Mouse Down", false));
    this.registerSetting(onlyWeapon = new ButtonSetting("only Weapon", false));
  }

  @Override
  public void onEnable() {
    packets.clear();
  }

  @SubscribeEvent
  public void onReceivePacket(ReceivePacketEvent e) {
  }

  @SubscribeEvent
  public void onSentPacket(SendPacketEvent e) {
  }

  private void flushOne() {
  }

  private void flushAll() {
  }

  private boolean conditionals() {
    return false;
  }
}
