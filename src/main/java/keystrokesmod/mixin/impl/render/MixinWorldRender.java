package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.Xray;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

@Mixin(WorldRenderer.class)
public class MixinWorldRender {
    @Redirect(method = "putColorMultiplier", at = @At(value = "INVOKE", target = "Ljava/nio/IntBuffer;put(II)Ljava/nio/IntBuffer;"))
    public IntBuffer translucent(final IntBuffer intBuffer, final int i, int j) {
        if (ModuleManager.xray != null && ModuleManager.xray.isEnabled() && ModuleManager.xray.translucent.isToggled()) {
            j = Xray.setXRayAlpha(j);
        }
        return intBuffer.put(i, j);
    }
}
