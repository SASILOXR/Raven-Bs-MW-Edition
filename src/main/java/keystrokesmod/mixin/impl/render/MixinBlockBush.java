package keystrokesmod.mixins.impl.render;

import keystrokesmod.module.ModuleManager;
import net.minecraft.block.BlockBush;
import net.minecraft.util.EnumWorldBlockLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBush.class)
public abstract class MixinBlockBush extends MixinBlock {
    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    public void translucent(CallbackInfoReturnable<EnumWorldBlockLayer> cir) {
        if (ModuleManager.xray != null && ModuleManager.xray.isEnabled() && ModuleManager.xray.translucent.isToggled()) {
            cir.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
        }
    }
}
