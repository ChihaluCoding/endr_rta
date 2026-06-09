package chihalu.endrrta.client.mixin;

import chihalu.endrrta.client.practice.PracticeScenarioWorldStarter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	private static final int PRACTICE_BUTTON_WIDTH = 112;
	private static final int PRACTICE_BUTTON_HEIGHT = 20;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init()V", at = @At("TAIL"))
	@SuppressWarnings("unused")
	private void endrrta$addPracticeWorldButtons(CallbackInfo ci) {
		Minecraft client = this.minecraft;
		if (client == null) {
			return;
		}

		int mainButtonX = this.width / 2 - 100;
		int x = Math.max(8, mainButtonX - PRACTICE_BUTTON_WIDTH - 14);
		int y = this.height / 4 + 48;
		addPracticeButton(client, x, y, "個別練習");
	}

	private void addPracticeButton(Minecraft client, int x, int y, String label) {
		addRenderableWidget(Button.builder(
				Component.literal(label),
				button -> PracticeScenarioWorldStarter.openPracticeHubWorld(client)
		).bounds(x, y, PRACTICE_BUTTON_WIDTH, PRACTICE_BUTTON_HEIGHT).build());
	}
}
