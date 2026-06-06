package chihalu.endrrta.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.client.reset.QuickResetHandler;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	private static final int SEED_RESET_WIDTH = 104;
	private static final int SEED_RESET_HEIGHT = 20;
	private static final int SEED_RESET_MARGIN = 8;

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void endrrta$addSeedResetButton(CallbackInfo info) {
		Minecraft client = this.minecraft;
		if (client == null || !client.hasSingleplayerServer() || !((PauseScreen) (Object) this).showsPauseMenu()) {
			return;
		}

		addRenderableWidget(Button.builder(
				Component.translatable("button.endrrta.seed_reset"),
				button -> QuickResetHandler.requestSeedReset(client, (Screen) (Object) this)
		).bounds(
				this.width - SEED_RESET_WIDTH - SEED_RESET_MARGIN,
				this.height - SEED_RESET_HEIGHT - SEED_RESET_MARGIN,
				SEED_RESET_WIDTH,
				SEED_RESET_HEIGHT
		).build());
	}
}
