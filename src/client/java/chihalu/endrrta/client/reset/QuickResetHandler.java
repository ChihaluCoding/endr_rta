package chihalu.endrrta.client.reset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfigManager;

public final class QuickResetHandler {
	private QuickResetHandler() {
	}

	public static void requestQuickReset(Minecraft minecraft) {
		requestReset(minecraft, null, Component.literal("EndrRTA クイックリセット"));
	}

	public static void requestSeedReset(Minecraft minecraft, @Nullable Screen cancelScreen) {
		requestReset(minecraft, cancelScreen, Component.translatable("button.endrrta.seed_reset"));
	}

	private static void requestReset(Minecraft minecraft, @Nullable Screen cancelScreen, Component title) {
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}
		if (EndrRTAConfigManager.get().confirmQuickReset) {
			minecraft.setScreen(new ConfirmScreen(
					confirmed -> {
						if (confirmed) {
							resetToNewWorld(minecraft);
						} else {
							minecraft.setScreen(cancelScreen);
						}
					},
					title,
					Component.literal("現在のワールドを終了して、新しいワールドを作成します。")
			));
			return;
		}
		resetToNewWorld(minecraft);
	}

	private static void resetToNewWorld(Minecraft minecraft) {
		Screen loadingScreen = new GenericMessageScreen(Component.translatable("selectWorld.data_read"));
		minecraft.disconnect(loadingScreen, true);
		SeedResetWorldStarter.startNewWorld(minecraft);
	}
}
