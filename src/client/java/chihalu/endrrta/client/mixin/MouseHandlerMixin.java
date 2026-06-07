package chihalu.endrrta.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.client.pie.PieChartAssistHandler;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void endrrta$selectPieChartByWheel(long window, double horizontalAmount, double verticalAmount, CallbackInfo info) {
		if (PieChartAssistHandler.handleScroll(Minecraft.getInstance(), verticalAmount)) {
			info.cancel();
		}
	}

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void endrrta$clickPieChartSelection(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo info) {
		if (PieChartAssistHandler.handleMouseButton(Minecraft.getInstance(), buttonInfo.button(), action)) {
			info.cancel();
		}
	}
}
