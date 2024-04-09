package keystrokesmod.module.impl.player;

import java.awt.Color;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Freecam extends Module {
   public static SliderSetting speed;
   public static ButtonSetting disableOnDamage;
   public static EntityOtherPlayerMP freeEntity = null;
   private int[] lcc = new int[]{Integer.MAX_VALUE, 0};
   private float[] sAng = new float[]{0.0F, 0.0F};

   public Freecam() {
      super("Freecam", Module.category.player, 0);
      this.registerSetting(speed = new SliderSetting("Speed", 2.5D, 0.5D, 10.0D, 0.5D));
      this.registerSetting(disableOnDamage = new ButtonSetting("Disable on damage", true));
   }

   public void onEnable() {
      if (!mc.thePlayer.onGround) {
         this.disable();
      } else {
         freeEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
         freeEntity.copyLocationAndAnglesFrom(mc.thePlayer);
         this.sAng[0] = freeEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
         this.sAng[1] = mc.thePlayer.rotationPitch;
         freeEntity.setVelocity(0.0D, 0.0D, 0.0D);
         freeEntity.setInvisible(true);
         mc.theWorld.addEntityToWorld(-8008, freeEntity);
         mc.setRenderViewEntity(freeEntity);
      }
   }

   public void onDisable() {
      if (freeEntity != null) {
         mc.setRenderViewEntity(mc.thePlayer);
         mc.thePlayer.rotationYaw = mc.thePlayer.rotationYawHead = this.sAng[0];
         mc.thePlayer.rotationPitch = this.sAng[1];
         mc.theWorld.removeEntity(freeEntity);
         freeEntity = null;
      }

      this.lcc = new int[]{Integer.MAX_VALUE, 0};
      int rg = 1;
      int x = mc.thePlayer.chunkCoordX;
      int z = mc.thePlayer.chunkCoordZ;

      for(int x2 = -1; x2 <= 1; ++x2) {
         for(int z2 = -1; z2 <= 1; ++z2) {
            int a = x + x2;
            int b = z + z2;
            mc.theWorld.markBlockRangeForRenderUpdate(a * 16, 0, b * 16, a * 16 + 15, 256, b * 16 + 15);
         }
      }

   }

   public void onUpdate() {
      if (disableOnDamage.isToggled() && mc.thePlayer.hurtTime != 0) {
         this.disable();
      } else {
         mc.thePlayer.setSprinting(false);
         mc.thePlayer.moveForward = 0.0F;
         mc.thePlayer.moveStrafing = 0.0F;
         freeEntity.rotationYaw = freeEntity.rotationYawHead = mc.thePlayer.rotationYaw;
         freeEntity.rotationPitch = mc.thePlayer.rotationPitch;
         double s = 0.215D * speed.getInput();
         EntityOtherPlayerMP var10000;
         double rad;
         double dx;
         double dz;
         if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            rad = (double) freeEntity.rotationYawHead * 0.017453292519943295D;
            dx = -1.0D * Math.sin(rad) * s;
            dz = Math.cos(rad) * s;
            var10000 = freeEntity;
            var10000.posX += dx;
            var10000 = freeEntity;
            var10000.posZ += dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
            rad = (double) freeEntity.rotationYawHead * 0.017453292519943295D;
            dx = -1.0D * Math.sin(rad) * s;
            dz = Math.cos(rad) * s;
            var10000 = freeEntity;
            var10000.posX -= dx;
            var10000 = freeEntity;
            var10000.posZ -= dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
            rad = (double)(freeEntity.rotationYawHead - 90.0F) * 0.017453292519943295D;
            dx = -1.0D * Math.sin(rad) * s;
            dz = Math.cos(rad) * s;
            var10000 = freeEntity;
            var10000.posX += dx;
            var10000 = freeEntity;
            var10000.posZ += dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
            rad = (double)(freeEntity.rotationYawHead + 90.0F) * 0.017453292519943295D;
            dx = -1.0D * Math.sin(rad) * s;
            dz = Math.cos(rad) * s;
            var10000 = freeEntity;
            var10000.posX += dx;
            var10000 = freeEntity;
            var10000.posZ += dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            var10000 = freeEntity;
            var10000.posY += 0.93D * s;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            var10000 = freeEntity;
            var10000.posY -= 0.93D * s;
         }

         mc.thePlayer.setSneaking(false);
         if (this.lcc[0] != Integer.MAX_VALUE && (this.lcc[0] != freeEntity.chunkCoordX || this.lcc[1] != freeEntity.chunkCoordZ)) {
            int x = freeEntity.chunkCoordX;
            int z = freeEntity.chunkCoordZ;
            mc.theWorld.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
         }

         this.lcc[0] = freeEntity.chunkCoordX;
         this.lcc[1] = freeEntity.chunkCoordZ;
      }
   }

   @SubscribeEvent
   public void re(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         mc.thePlayer.renderArmPitch = mc.thePlayer.prevRenderArmPitch = 700.0F;
         RenderUtils.renderEntity(mc.thePlayer, 1, 0.0D, 0.0D, Color.green.getRGB(), false);
         RenderUtils.renderEntity(mc.thePlayer, 2, 0.0D, 0.0D, Color.green.getRGB(), false);
      }

   }

   @SubscribeEvent
   public void m(MouseEvent e) {
      if (Utils.nullCheck() && e.button != -1) {
         e.setCanceled(true);
      }

   }
}
