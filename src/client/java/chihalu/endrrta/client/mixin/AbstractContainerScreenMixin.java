package chihalu.endrrta.client.mixin;

import chihalu.endrrta.client.inventory.IgnoredPickupItemRegistrar;
import chihalu.endrrta.server.PracticeMenuClickPayload;
import chihalu.endrrta.server.PracticeSelectionMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	private static final int END_RTA_LABEL_COLOR = -12566464;
	private static final String END_RTA_TITLE_MENU = "メニュー";
	private static final String END_RTA_TITLE_GAME_SELECTION = "ゲーム選択";
	private static final String END_RTA_TITLE_BASTION = "ピグリン要塞";

	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Shadow
	protected int titleLabelX;

	@Shadow
	protected int titleLabelY;

	@Shadow
	protected int inventoryLabelX;

	@Shadow
	protected int inventoryLabelY;

	@Shadow
	@Final
	protected Component playerInventoryTitle;

	private int endrrta$pendingPracticeMenuSlot = -1;

	@Invoker("getHoveredSlot")
	protected abstract Slot endrrta$getHoveredSlot(double mouseX, double mouseY);

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void endrrta$registerIgnoredPickupItem(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> info) {
		if (endrrta$handlePracticeDisplaySlotClick(event, info)) {
			return;
		}
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

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void endrrta$releasePracticeDisplaySlot(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || endrrta$pendingPracticeMenuSlot < 0) {
			return;
		}
		if (ClientPlayNetworking.canSend(PracticeMenuClickPayload.TYPE)) {
			ClientPlayNetworking.send(new PracticeMenuClickPayload(endrrta$pendingPracticeMenuSlot));
		}
		endrrta$pendingPracticeMenuSlot = -1;
		info.setReturnValue(true);
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void endrrta$dragPracticeDisplaySlot(MouseButtonEvent event, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> info) {
		if (endrrta$pendingPracticeMenuSlot >= 0) {
			info.setReturnValue(true);
		}
	}

	private boolean endrrta$handlePracticeDisplaySlotClick(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !(menu instanceof PracticeSelectionMenu practiceMenu)) {
			return false;
		}

		Slot slot = endrrta$getHoveredSlot(event.x(), event.y());
		if (slot == null || !practiceMenu.isDisplaySlot(slot.index)) {
			return false;
		}

		endrrta$pendingPracticeMenuSlot = slot.index;
		info.setReturnValue(true);
		return true;
	}

	@Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
	private void endrrta$extractPracticeMenuLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo info) {
		Component practiceTitle = endrrta$practiceMenuTitleFromSlots();
		if (practiceTitle == null) {
			return;
		}
		graphics.text(Minecraft.getInstance().font, practiceTitle, titleLabelX, titleLabelY, END_RTA_LABEL_COLOR, false);
		graphics.text(Minecraft.getInstance().font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, END_RTA_LABEL_COLOR, false);
		info.cancel();
	}

	private Component endrrta$practiceMenuTitleFromSlots() {
		String screenTitle = ((Screen) (Object) this).getTitle().getString();
		if (!END_RTA_TITLE_MENU.equals(screenTitle)
				&& !END_RTA_TITLE_GAME_SELECTION.equals(screenTitle)
				&& !END_RTA_TITLE_BASTION.equals(screenTitle)) {
			return null;
		}

		ItemStack slot10 = endrrta$getMenuItem(10);
		if (slot10.is(Items.DIAMOND_SWORD)) {
			return Component.literal(END_RTA_TITLE_MENU);
		}
		if (slot10.is(Items.CRIMSON_DOOR) || endrrta$getMenuItem(40).is(Items.COMPASS)) {
			return Component.literal(END_RTA_TITLE_BASTION);
		}
		return Component.literal(END_RTA_TITLE_GAME_SELECTION);
	}

	private ItemStack endrrta$getMenuItem(int slotIndex) {
		if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
			return ItemStack.EMPTY;
		}
		return menu.getSlot(slotIndex).getItem();
	}

}
