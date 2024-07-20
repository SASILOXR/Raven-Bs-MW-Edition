package keystrokesmod.mixins.impl.entity;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @ModifyVariable(method = "moveEntity", at = @At(value = "STORE", ordinal = 0), name = "flag")
    private boolean injectSafeWalk(boolean flag) {
        Entity entity = (Entity) (Object) this;
        Minecraft mc = Minecraft.getMinecraft();

        if (entity == mc.thePlayer && entity.onGround) {
            if (SafeWalk.canSafeWalk() || ModuleManager.scaffold.safewalk()) {
                return true;
            }
        }
        return flag;
    }
}