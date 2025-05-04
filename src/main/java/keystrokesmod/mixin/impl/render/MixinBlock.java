package keystrokesmod.mixins.impl.render;

import keystrokesmod.module.ModuleManager;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Shadow
    public abstract Vec3 modifyAcceleration(World worldIn, BlockPos pos, Entity entityIn, Vec3 motion);

    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    public void translucent(final CallbackInfoReturnable<EnumWorldBlockLayer> cir) {
        if (ModuleManager.xray != null && ModuleManager.xray.isEnabled() && ModuleManager.xray.translucent.isToggled()) {
            cir.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
        }
    }
}
