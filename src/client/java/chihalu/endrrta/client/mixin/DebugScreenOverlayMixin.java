package chihalu.endrrta.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DebugScreenOverlay.class, remap = false)
public abstract class DebugScreenOverlayMixin {
	@Shadow(remap = false)
	private boolean renderProfilerChart;

	@Shadow(remap = false)
	@Final
	private Minecraft minecraft;

	@Inject(method = "showProfilerChart", at = @At("HEAD"), cancellable = true, remap = false)
	private void endrrta$keepProfilerVisibleAfterDebugClose(CallbackInfoReturnable<Boolean> info) {
		if (renderProfilerChart) {
			info.setReturnValue(!minecraft.options.hideGui || minecraft.screen != null);
		}
	}
}
