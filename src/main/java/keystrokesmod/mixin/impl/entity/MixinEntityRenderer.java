package keystrokesmod.mixin.impl.entity;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AimAssist;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @ModifyVariable(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getLook(F)Lnet/minecraft/util/Vec3;"), ordinal = 1)
    public double getEntity(double value) {
        if (ModuleManager.aimAssist.isEnabled() && ModuleManager.aimAssist.hitThroughBlock.isToggled() && AimAssist.targetEntity != null) {
            return 2.99D;
        }
        return value;
    }
}
