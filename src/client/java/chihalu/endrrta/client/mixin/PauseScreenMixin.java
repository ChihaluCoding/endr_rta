package chihalu.endrrta.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import chihalu.endrrta.client.config.EndrRTAOptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.client.reset.QuickResetHandler;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	private static final int RTA_BUTTON_HEIGHT = 20;
	private static final int RTA_BUTTON_GAP = 4;
	private static final int RTA_BUTTON_COLUMN_GAP = 8;
	private static final int RTA_BUTTON_INSERT_HEIGHT = RTA_BUTTON_HEIGHT + RTA_BUTTON_GAP;

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init()V", at = @At("TAIL"))
	@SuppressWarnings("unused")
	private void endrrta$addSeedResetButton(CallbackInfo ci) {
		Minecraft client = this.minecraft;
		if (client == null || !client.hasSingleplayerServer() || !((PauseScreen) (Object) this).showsPauseMenu()) {
			return;
		}

		Button disconnectButton = ((PauseScreenAccessor) this).endrrta$disconnectButton();
		int buttonWidth = disconnectButton == null ? 204 : disconnectButton.getWidth();
		int buttonX = disconnectButton == null ? this.width / 2 - buttonWidth / 2 : disconnectButton.getX();
		int rtaButtonY = disconnectButton == null ? this.height / 4 + 120 : disconnectButton.getY();
		if (disconnectButton != null) {
			disconnectButton.setY(disconnectButton.getY() + RTA_BUTTON_INSERT_HEIGHT);
		}
		int halfWidth = (buttonWidth - RTA_BUTTON_COLUMN_GAP) / 2;

		addRenderableWidget(Button.builder(
				Component.translatable("button.endrrta.seed_reset"),
				button -> QuickResetHandler.requestSeedReset(client, (Screen) (Object) this)
		).bounds(
				buttonX,
				rtaButtonY,
				halfWidth,
				RTA_BUTTON_HEIGHT
		).build());

		addRenderableWidget(Button.builder(
				Component.literal("RTA設定"),
				button -> this.minecraft.setScreen(new EndrRTAOptionsScreen((Screen) (Object) this))
		).bounds(
				buttonX + halfWidth + RTA_BUTTON_COLUMN_GAP,
				rtaButtonY,
				halfWidth,
				RTA_BUTTON_HEIGHT
		).build());
	}
}
