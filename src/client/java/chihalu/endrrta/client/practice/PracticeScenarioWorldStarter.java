package chihalu.endrrta.client.practice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import chihalu.endrrta.EndrRTA;
import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;

public final class PracticeScenarioWorldStarter {
	private PracticeScenarioWorldStarter() {
	}

	public static void openPracticeHubWorld(Minecraft minecraft) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		config.practiceMode = true;
		config.practiceScenario = "none";
		EndrRTAConfigManager.save();

		if (!ensurePracticeHubWorldAvailable(minecraft, config)) {
			return;
		}
		PracticeHubClientState.markActive();
		minecraft.createWorldOpenFlows().openWorld(config.practiceHubWorldId, () -> minecraft.setScreen(null));
	}

	private static boolean ensurePracticeHubWorldAvailable(Minecraft minecraft, EndrRTAConfig config) {
		Path target = minecraft.gameDirectory.toPath().resolve("saves").resolve(config.practiceHubWorldId);
		if (Files.exists(target.resolve("level.dat"))) {
			return true;
		}

		Path source = Path.of(config.practiceHubTemplatePath);
		if (!Files.isDirectory(source)) {
			minecraft.setScreen(new GenericMessageScreen(Component.literal(
					"個別練習ワールドが見つかりません: " + source
			)));
			return false;
		}

		try {
			if (Files.exists(target)) {
				EndrRTA.LOGGER.warn("不完全な個別練習ワールドを検出したため、テンプレートから再コピーします: {}", target);
				deleteDirectory(target);
			}
			copyDirectory(source, target);
			return true;
		} catch (IOException exception) {
			EndrRTA.LOGGER.warn("個別練習ワールドをコピーできませんでした: {} -> {}", source, target, exception);
			minecraft.setScreen(new GenericMessageScreen(Component.literal(
					"個別練習ワールドをコピーできませんでした: " + source
			)));
			return false;
		}
	}

	private static void copyDirectory(Path source, Path target) throws IOException {
		try (var paths = Files.walk(source)) {
			for (Path path : paths.toList()) {
				Path relative = source.relativize(path);
				Path destination = target.resolve(relative);
				if (Files.isDirectory(path)) {
					Files.createDirectories(destination);
				} else {
					Files.copy(path, destination);
				}
			}
		}
	}

	private static void deleteDirectory(Path target) throws IOException {
		try (var paths = Files.walk(target)) {
			for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}
}
