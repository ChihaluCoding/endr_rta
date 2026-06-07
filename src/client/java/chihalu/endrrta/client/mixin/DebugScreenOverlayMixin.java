package chihalu.endrrta.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	@Shadow
	private boolean renderProfilerChart;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "showProfilerChart", at = @At("HEAD"), cancellable = true)
	private void endrrta$keepProfilerVisibleAfterDebugClose(CallbackInfoReturnable<Boolean> info) {
		if (renderProfilerChart) {
			info.setReturnValue(!minecraft.options.hideGui || minecraft.screen != null);
		}
	}
}
