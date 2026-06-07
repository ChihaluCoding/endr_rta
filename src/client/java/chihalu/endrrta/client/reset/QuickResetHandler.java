package chihalu.endrrta.client.reset;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.EndrRTA;
import chihalu.endrrta.client.mixin.MinecraftServerAccessor;
import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class QuickResetHandler {
	private QuickResetHandler() {
	}

	public static void requestQuickReset(Minecraft minecraft) {
		requestReset(minecraft, null, Component.literal("EnderRTA クイックリセット"));
	}

	public static void requestSeedReset(Minecraft minecraft, @Nullable Screen cancelScreen) {
		requestReset(minecraft, cancelScreen, Component.translatable("button.endrrta.seed_reset"));
	}

	private static void requestReset(Minecraft minecraft, @Nullable Screen cancelScreen, Component title) {
		if (minecraft.player == null || minecraft.level == null) {
			return;
		}
		EndrRTAConfig config = EndrRTAConfigManager.get();
		if (config.confirmQuickReset) {
			minecraft.setScreen(new ConfirmScreen(
					confirmed -> {
						if (confirmed) {
							resetToNewWorld(minecraft);
						} else {
							minecraft.setScreen(cancelScreen);
						}
					},
					title,
					resetConfirmMessage(config)
			));
			return;
		}
		resetToNewWorld(minecraft);
	}

	private static Component resetConfirmMessage(EndrRTAConfig config) {
		if (config.resetKeepPreviousWorld) {
			return Component.literal("現在のワールドを終了して、新しいワールドを作成します。");
		}
		return Component.literal("現在のワールドを終了して削除し、新しいワールドを作成します。");
	}

	private static void resetToNewWorld(Minecraft minecraft) {
		@Nullable String previousLevelId = EndrRTAConfigManager.get().resetKeepPreviousWorld ? null : currentLevelId(minecraft);
		Screen loadingScreen = new GenericMessageScreen(Component.translatable("selectWorld.data_read"));
		minecraft.disconnect(loadingScreen, true);
		deletePreviousWorldIfNeeded(minecraft, previousLevelId);
		SeedResetWorldStarter.startNewWorld(minecraft);
	}

	private static @Nullable String currentLevelId(Minecraft minecraft) {
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server == null) {
			return null;
		}
		return ((MinecraftServerAccessor) server).endrrta$getStorageSource().getLevelId();
	}

	private static void deletePreviousWorldIfNeeded(Minecraft minecraft, @Nullable String levelId) {
		if (levelId == null || levelId.isBlank()) {
			return;
		}
		try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(levelId)) {
			access.deleteLevel();
		} catch (IOException exception) {
			EndrRTA.LOGGER.warn("シードリセット前のワールドを削除できませんでした: {}", levelId, exception);
		}
	}
}
