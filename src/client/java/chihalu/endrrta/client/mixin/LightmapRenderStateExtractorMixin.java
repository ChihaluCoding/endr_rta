package chihalu.endrrta.client.mixin;

import chihalu.endrrta.config.EndrRTAConfigManager;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private void endrrta$applyFullBright(LightmapRenderState state, float partialTick, CallbackInfo info) {
		if (!EndrRTAConfigManager.get().allowsFullBright()) {
			return;
		}

		state.brightness = 1.0F;
		state.darknessEffectScale = 0.0F;
		state.nightVisionEffectIntensity = 1.0F;
		state.bossOverlayWorldDarkening = 0.0F;
	}
}
