package keystrokesmod.module.impl.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public class Settings extends Module {
  public static SliderSetting customCapes;
  public static ButtonSetting weaponAxe;
  public static ButtonSetting weaponRod;
  public static ButtonSetting weaponStick;
  public static ButtonSetting middleClickFriends;
  public static ButtonSetting setChatAsInventory;

  public static ButtonSetting checkColor;
  public static ButtonSetting checkPrefix;
  public static ButtonSetting checkVanillaTeam;

  public static ButtonSetting rotateBody;
  public static ButtonSetting fullBody;
  public static ButtonSetting movementFix;
  public static ButtonSetting strictMove;
  public static SliderSetting randomYawFactor;

  public static ButtonSetting loadGuiPositions;
  public static ButtonSetting sendMessage;

  public static SliderSetting offset;
  public static SliderSetting timeMultiplier;

  private String[] capes =
      new String[] {"None", "Anime", "Aqua", "Green", "Purple", "Red", "White", "Yellow", "Custom"};
  public File directory;

  public static List<ResourceLocation> loadedCapes = new ArrayList<>();

  public Settings() {
    super("Settings", category.client, 0);
    this.registerSetting(new DescriptionSetting("General"));
    this.registerSetting(customCapes = new SliderSetting("Custom cape", 0, capes));
    this.registerSetting(weaponAxe = new ButtonSetting("Set axe as weapon", false));
    this.registerSetting(weaponRod = new ButtonSetting("Set rod as weapon", false));
    this.registerSetting(weaponStick = new ButtonSetting("Set stick as weapon", false));
    this.registerSetting(middleClickFriends = new ButtonSetting("Middle click friends", false));
    this.registerSetting(setChatAsInventory = new ButtonSetting("Set chat as inventory", false));
    this.registerSetting(new DescriptionSetting("Rotations"));
    this.registerSetting(rotateBody = new ButtonSetting("Rotate body", true));
    this.registerSetting(fullBody = new ButtonSetting("Full body", false));
    this.registerSetting(movementFix = new ButtonSetting("Movement fix", false));
    this.registerSetting(strictMove = new ButtonSetting("StrictMove", true));
    this.registerSetting(new DescriptionSetting("Teammates Check"));
    this.registerSetting(checkColor = new ButtonSetting("Check teammates by color", true));
    this.registerSetting(checkPrefix = new ButtonSetting("Check teammates by prefix", true));
    this.registerSetting(
        checkVanillaTeam = new ButtonSetting("Check teammates by vanilla team", false));
    this.registerSetting(
        randomYawFactor = new SliderSetting("Random yaw factor", 1.0, 0.0, 10.0, 1.0));
    this.registerSetting(new DescriptionSetting("Profiles"));
    this.registerSetting(loadGuiPositions = new ButtonSetting("Load gui state", false));
    this.registerSetting(sendMessage = new ButtonSetting("Send message on enable", true));
    this.registerSetting(new DescriptionSetting("Theme colors"));
    this.registerSetting(offset = new SliderSetting("Offset", 0.5, -3.0, 3.0, 0.1));
    this.registerSetting(timeMultiplier = new SliderSetting("Time multiplier", 0.5, 0.1, 4.0, 0.1));
    this.canBeEnabled = false;
    directory = new File(mc.mcDataDir + File.separator + "capes");
    if (!directory.exists()) {
      boolean success = directory.mkdirs();
      if (!success) {
        System.out.println("There was an issue creating capes directory.");
        return;
      }
    }
    if (directory.listFiles().length == 0) {
      System.out.println("None Load Custom Cape");
    }
    loadCapes(directory.listFiles());
  }

  public void loadCapes(File[] CustomCapes) {
    try {
      for (int i = 1; i < capes.length; i++) {
        if (capes[i].equals("Custom")) {
          System.out.println("Load Custom Cape");
          if (CustomCapes.length == 0) {
            loadedCapes.add(null);
            continue;
          }
          InputStream stream = Files.newInputStream(CustomCapes[0].toPath());
          BufferedImage bufferedImage = ImageIO.read(stream);
          loadedCapes.add(
              mc.renderEngine.getDynamicTextureLocation(
                  "Custom", new DynamicTexture(bufferedImage)));
        } else {
          String name = capes[i].toLowerCase();
          if (i > 1) {
            name = "rvn_" + name;
          }
          InputStream stream =
              Raven.class.getResourceAsStream(
                  "/assets/keystrokesmod/textures/capes/" + name + ".png");
          if (stream == null) {
            continue;
          }
          BufferedImage bufferedImage = ImageIO.read(stream);
          loadedCapes.add(
              mc.renderEngine.getDynamicTextureLocation(name, new DynamicTexture(bufferedImage)));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static boolean inInventory() {
    if (mc.currentScreen instanceof GuiInventory) {
      return true;
    }
    if (mc.currentScreen instanceof GuiChat && setChatAsInventory.isToggled()) {
      return true;
    }
    return false;
  }
}
