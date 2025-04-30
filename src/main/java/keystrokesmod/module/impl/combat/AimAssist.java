package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import javax.rmi.CORBA.Util;
import javax.swing.plaf.SplitPaneUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class AimAssist extends Module {
    private SliderSetting speed;
    private SliderSetting fov;
    private SliderSetting distance;
    private ButtonSetting clickAim;
    private ButtonSetting weaponOnly;
    private ButtonSetting aimInvis;
    private ButtonSetting blatantMode;
    private ButtonSetting ignoreTeammates;
    private ButtonSetting rangeFirst;
    private ButtonSetting aimNecessary;
    public static EntityLivingBase targetEntity;
    private static boolean shouldRender;

    public AimAssist() {
        super("AimAssist", category.combat, 0);
        this.registerSetting(speed = new SliderSetting("Speed", 45.0D, 1.0D, 100.0D, 1.0D));
        this.registerSetting(fov = new SliderSetting("FOV", 90.0D, 15.0D, 180.0D, 1.0D));
        this.registerSetting(distance = new SliderSetting("Distance", 4.5D, 1.0D, 10.0D, 0.5D));
        this.registerSetting(clickAim = new ButtonSetting("Click aim", true));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(aimInvis = new ButtonSetting("Aim invis", false));
        this.registerSetting(blatantMode = new ButtonSetting("Blatant mode", false));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", false));
        this.registerSetting(rangeFirst = new ButtonSetting("Range First", false));
        this.registerSetting(aimNecessary = new ButtonSetting("Aim Necessary", false));
    }

    public void onUpdate() {
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
                        } else if (speed.getInput() == 100){
                            Utils.aim(en, 0.0F, false);
                        } else {
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

    private Entity getEnemy() {
        final int n = (int)fov.getInput();
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
                if (mc.thePlayer.getDistanceToEntity(entityPlayer) > distance.getInput()) {
                    continue;
                }
                if (AntiBot.isBot(entityPlayer)) {
                    continue;
                }
                if (!blatantMode.isToggled() && n != 360 && !Utils.inFov((float)n, entityPlayer)) {
                    continue;
                }
                if (aimNecessary.isToggled() && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                    targetEntity = (EntityLivingBase) mc.objectMouseOver.entityHit;
                    shouldRender = true;
                    return null;
                }
                if (!rangeFirst.isToggled()) {
                    return entityPlayer;
                }
                players.add(entityPlayer);
            }
        }
        if (!players.isEmpty()) {
            Entity selectEntity = players.stream().min(Comparator.comparingDouble(entity -> mc.thePlayer.getDistanceToEntity(entity))).get();
            return selectEntity;
        }
        shouldRender = false;
        return null;
    }
}
