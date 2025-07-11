package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MegaWallsItemESP extends Module {
  private ButtonSetting phxLoot,
      squLoot,
      pirLoot,
      renLoot,
      cowLoot,
      molLoot,
      molLoot1,
      hunLoot,
      diamond,
      diamondSword,
      diamondArmor;
  private SliderSetting itemTagScale;

  public MegaWallsItemESP() {
    super("MegaWallsItemESP", category.render);
    this.registerSetting(itemTagScale = new SliderSetting("ItemTagScale", 0.2, 0.1, 1, 0.1));
    this.registerSetting(phxLoot = new ButtonSetting("Render phxLoot", true));
    this.registerSetting(squLoot = new ButtonSetting("Render squLoot", true));
    this.registerSetting(pirLoot = new ButtonSetting("Render pirLoot", true));
    this.registerSetting(renLoot = new ButtonSetting("Render renLoot", true));
    this.registerSetting(cowLoot = new ButtonSetting("Render cowLoot", true));
    this.registerSetting(molLoot = new ButtonSetting("Render junkApple", true));
    this.registerSetting(molLoot1 = new ButtonSetting("Render pie", true));
    this.registerSetting(hunLoot = new ButtonSetting("Render hunLoot", true));
    this.registerSetting(diamond = new ButtonSetting("Render diamond", true));
    this.registerSetting(diamondArmor = new ButtonSetting("Render diamondArmor", true));
    this.registerSetting(diamondSword = new ButtonSetting("Render diamondSword", true));
  }

  @SubscribeEvent
  public void render(RenderWorldLastEvent event) {
    for (Entity entity : mc.theWorld.loadedEntityList) {
      if (entity instanceof EntityItem) {
        EntityItem entityItem = (EntityItem) entity;
        ItemStack itemStack = entityItem.getEntityItem();
        Item item = itemStack.getItem();
        String itemName = itemStack.getDisplayName();
        String stripItemName = StringUtils.stripControlCodes(itemName);
        int stackSize = itemStack.stackSize;
        if (stripItemName.startsWith("Phoenix's Tears of Regen") && phxLoot.isToggled()) {
          this.renderItemTag(
              entity,
              "§6Phoenix's Tears of Regen §fx" + stackSize,
              -1,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (stripItemName.startsWith("Squid's Absorption") && squLoot.isToggled()) {
          this.renderItemTag(
              entity,
              "§9Squid's Absorption §fx" + stackSize,
              -1,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (stripItemName.startsWith("Matey") && pirLoot.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              -1,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (stripItemName.startsWith("Regen-Ade") && renLoot.isToggled()) {
          this.renderItemTag(
              entity,
              "§bRegen-ades §fx" + stackSize,
              -1,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (stripItemName.startsWith("Ultra Pasteurized Milk Bucket") && cowLoot.isToggled()) {
          this.renderItemTag(
              entity,
              "§fMilk Bucket §fx" + stackSize,
              -1,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (stripItemName.startsWith("Junk Apple") && molLoot.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              -1,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (item == Items.pumpkin_pie && molLoot1.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              16711610,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (item == Items.golden_apple && hunLoot.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              16776260,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (item == Items.diamond && diamond.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              3132391,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (item == Items.diamond_sword && diamondSword.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              3132391,
              event.partialTicks,
              itemTagScale.getInput());
        }

        if (this.isDiamondArmor(item) && diamondArmor.isToggled()) {
          this.renderItemTag(
              entity,
              itemName + " §fx" + stackSize,
              3132391,
              event.partialTicks,
              itemTagScale.getInput());
        }
      }
    }
  }

  private boolean isDiamondArmor(Item item) {
    return item == Items.diamond_boots
        || item == Items.diamond_leggings
        || item == Items.diamond_helmet
        || item == Items.diamond_chestplate;
  }

  private void renderItemTag(
      Entity entity, String text, int color, float partialTicks, double scale) {
    RenderUtils.renderItemTag(entity, text, color, partialTicks, (float) scale);
  }
}
