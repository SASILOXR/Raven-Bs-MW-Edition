package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import keystrokesmod.Raven;
import keystrokesmod.event.ClientLookEvent;
import keystrokesmod.event.RotationEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AimAssist extends Module {
  private SliderSetting mode;
  private SliderSetting speed;
  private SliderSetting fov;
  private SliderSetting distance;
  private SliderSetting jitter;
  private ButtonSetting clickAim;
  private ButtonSetting weaponOnly;
  private ButtonSetting aimInvis;
  private ButtonSetting blatantMode;
  private ButtonSetting ignoreTeammates;
  private ButtonSetting rangeFirst;
  private ButtonSetting fovPriority;
  private ButtonSetting aimNecessary;
  private ButtonSetting norotation;
  public ButtonSetting hitThroughBlock;
  public static EntityLivingBase targetEntity;
  private static boolean shouldRender;
  private static Float[] lookAt = null;
  private static Random rand = null;
  private static boolean rotated;

  private String[] modes = new String[] {"Vanilla", "Silent"};

  public AimAssist() {
    super("AimAssist", category.combat, 0);
    this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
    this.registerSetting(speed = new SliderSetting("Speed", 45.0D, 1.0D, 100.0D, 1.0D));
    this.registerSetting(fov = new SliderSetting("FOV", 90.0D, 15.0D, 180.0D, 1.0D));
    this.registerSetting(jitter = new SliderSetting("Jitter", 0.0, 0.0, 3.0, 0.1));
    this.registerSetting(distance = new SliderSetting("Distance", 4.5D, 1.0D, 10.0D, 0.5D));
    this.registerSetting(clickAim = new ButtonSetting("Click aim", true));
    this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
    this.registerSetting(aimInvis = new ButtonSetting("Aim invis", false));
    this.registerSetting(blatantMode = new ButtonSetting("Blatant mode", false));
    this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", false));
    this.registerSetting(rangeFirst = new ButtonSetting("Range First", false));
    this.registerSetting(fovPriority = new ButtonSetting("Fov Priority", false));
    this.registerSetting(aimNecessary = new ButtonSetting("Aim Necessary", false));
    this.registerSetting(norotation = new ButtonSetting("No Rotation", true));
    this.registerSetting(hitThroughBlock = new ButtonSetting("Hit Through Block", false));
  }

  public void onEnable() {
    rand = new Random();
  }

  public void onDisable() {
    targetEntity = null;
    rotated = false;
  }

  @Override
  public String getInfo() {
    return modes[(int) mode.getInput()];
  }

  public void onUpdate() {
    if (mode.getInput() != 0) {
      return;
    }
    if (mc.currentScreen == null && mc.inGameHasFocus) {
      if (!weaponOnly.isToggled() || Utils.holdingWeapon()) {
        if (!clickAim.isToggled() || Utils.isClicking()) {
          Entity en = this.getEnemy();
          if (en != null) {
            if (en instanceof EntityLivingBase) {
              targetEntity = (EntityLivingBase) en;
            }
            if (Raven.debug) {
              Utils.sendMessage(this.getName() + " &e" + en.getName());
            }
            if (blatantMode.isToggled()) {
              Utils.aim(en, 0.0F, false);
            } else if (speed.getInput() == 100) {
              Utils.aim(en, 0.0F, false);
            } else if (mode.getInput() == 0) {
              double n = Utils.n(en);
              if (n > 1.0D || n < -1.0D) {
                float val = (float) (-(n / (100.0D - (speed.getInput()))));
                mc.thePlayer.rotationYaw += val;
              }
            }
            return;
          }
          if (shouldRender) {
            return;
          }
        }
      }
    }
    targetEntity = null;
  }

  @SubscribeEvent
  public void onRotated(RotationEvent event) {
    lookAt = null;
    if (mode.getInput() != 1) {
      return;
    }

    if (mc.currentScreen == null && mc.inGameHasFocus) {
      if (!weaponOnly.isToggled() || Utils.holdingWeapon()) {
        if (!clickAim.isToggled() || Utils.isClicking()) {
          Entity entity = this.getEnemy();
          if (entity != null) {
            if (entity instanceof EntityLivingBase) {
              targetEntity = (EntityLivingBase) entity;
            }
            if (speed.getInput() == 100) {
              float[] rotations = RotationUtils.getRotations(entity);
              if (rotations != null) {
                float yaw = rotations[0];
                float pitch = MathHelper.clamp_float(rotations[1] + 4.0F, -90, 90);

                if (jitter.getInput() > 0.0D) {
                  double jitterAmount = jitter.getInput() * 0.45D;
                  if (rand.nextBoolean()) {
                    yaw += rand.nextFloat() * jitterAmount;
                  } else {
                    yaw -= rand.nextFloat() * jitterAmount;
                  }
                  if (rand.nextBoolean()) {
                    pitch += rand.nextFloat() * jitterAmount * 0.45D;
                  } else {
                    pitch -= rand.nextFloat() * jitterAmount * 0.45D;
                  }
                }

                event.setPitch(pitch);
                event.setYaw(yaw);
                lookAt = new Float[] {yaw, pitch};
                rotated = true;
              }

            } else {
              double n = Utils.aimDiff(entity, true);
              float val = (float) (-(n / (100.0D - (speed.getInput()))));
              event.setYaw(RotationUtils.serverRotations[0] + val);
              lookAt = new Float[] {RotationUtils.serverRotations[0] + val};
              rotated = true;
            }
            return;
          }
          if (shouldRender) {
            float yaw = RotationUtils.serverRotations[0];
            float pitch = RotationUtils.serverRotations[1];
            event.setYaw(yaw);
            event.setPitch(pitch);
            lookAt = new Float[] {yaw, pitch};
            rotated = true;
            return;
          }
        }
      }
    }
    targetEntity = null;

    if (!norotation.isToggled() && rotated) {
      mc.thePlayer.rotationYaw = RotationUtils.serverRotations[0];
      mc.thePlayer.rotationPitch = RotationUtils.serverRotations[1];
      rotated = false;
    }
  }

  @SubscribeEvent
  public void onClientLook(ClientLookEvent event) {
    if (lookAt != null) {
      if (lookAt.length == 2) {
        if (lookAt[1] != null) {
          event.pitch = lookAt[1];
        }
      }
      if (lookAt[0] != null) {
        event.yaw = lookAt[0];
      }
    }
  }

  private Entity getEnemy() {
    final int n = (int) fov.getInput();
    ArrayList<Entity> players = new ArrayList<>();
    for (final EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
      if (entityPlayer != mc.thePlayer && entityPlayer.deathTime == 0) {
        if (Utils.isFriended(entityPlayer)) {
          continue;
        }
        if (ignoreTeammates.isToggled() && Utils.isTeamMate(entityPlayer)) {
          continue;
        }
        if (!aimInvis.isToggled() && entityPlayer.isInvisible()) {
          continue;
        }
        if (getDistanceToBoundingBox(entityPlayer) > distance.getInput()) {
          continue;
        }
        if (AntiBot.isBot(entityPlayer)) {
          continue;
        }
        if (!blatantMode.isToggled() && n != 360 && !Utils.inFov((float) n, entityPlayer)) {
          continue;
        }
        if (aimNecessary.isToggled()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
          targetEntity = (EntityLivingBase) mc.objectMouseOver.entityHit;
          shouldRender = true;
          return null;
        }
        players.add(entityPlayer);
      }
    }
    if (!players.isEmpty()) {

      if (!(fovPriority.isToggled() || rangeFirst.isToggled())) {
        return players.get(0);
      }

      if (fovPriority.isToggled()) {
        Entity selectEntity =
            players.stream().min(Comparator.comparingDouble(Utils::fovOffset)).get();
        return selectEntity;
      }

      if (rangeFirst.isToggled()) {
        Entity selectEntity =
            players.stream()
                .min(Comparator.comparingDouble(entity -> mc.thePlayer.getDistanceToEntity(entity)))
                .get();
        return selectEntity;
      }
    }
    shouldRender = false;
    return null;
  }

  private double getDistanceToBoundingBox(Entity target) {
    if (mc.thePlayer == null) {
      return 0;
    }
    Vec3 playerEyePos =
        mc.thePlayer.getPositionEyes(((IAccessorMinecraft) mc).getTimer().renderPartialTicks);
    AxisAlignedBB boundingBox = target.getEntityBoundingBox();
    double nearestX =
        MathHelper.clamp_double(playerEyePos.xCoord, boundingBox.minX, boundingBox.maxX);
    double nearestY =
        MathHelper.clamp_double(playerEyePos.yCoord, boundingBox.minY, boundingBox.maxY);
    double nearestZ =
        MathHelper.clamp_double(playerEyePos.zCoord, boundingBox.minZ, boundingBox.maxZ);
    Vec3 nearestPoint = new Vec3(nearestX, nearestY, nearestZ);
    return playerEyePos.distanceTo(nearestPoint);
  }
}
