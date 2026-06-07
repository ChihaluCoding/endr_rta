package chihalu.endrrta.client.mixin;

import chihalu.endrrta.client.inventory.IgnoredPickupItemRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Invoker("getHoveredSlot")
	protected abstract Slot endrrta$getHoveredSlot(double mouseX, double mouseY);

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void endrrta$registerIgnoredPickupItem(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> info) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !event.hasControlDown()) {
			return;
		}

		Slot slot = endrrta$getHoveredSlot(event.x(), event.y());
		if (slot == null || !slot.hasItem()) {
			return;
		}

		if (IgnoredPickupItemRegistrar.registerHoveredStack(Minecraft.getInstance(), slot.getItem())) {
			info.setReturnValue(true);
		}
	}
}
