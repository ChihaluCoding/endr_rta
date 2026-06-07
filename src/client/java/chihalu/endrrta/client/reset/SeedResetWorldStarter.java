package chihalu.endrrta.client.reset;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class SeedResetWorldStarter {
	private static final DateTimeFormatter WORLD_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

	private SeedResetWorldStarter() {
	}

	public static void startNewWorld(Minecraft minecraft) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		String worldName = "EnderRTA-Reset-" + LocalDateTime.now().format(WORLD_NAME_FORMAT)
				+ "-" + UUID.randomUUID().toString().substring(0, 8);
		LevelSettings levelSettings = new LevelSettings(
				worldName,
				GameType.SURVIVAL,
				new LevelSettings.DifficultySettings(Difficulty.NORMAL, config.resetHardcore, false),
				config.allowsResetCommands(),
				WorldDataConfiguration.DEFAULT
		);
		WorldOptions worldOptions = WorldOptions.defaultWithRandomSeed()
				.withStructures(config.resetGenerateStructures)
				.withBonusChest(config.allowsResetBonusChest());

		minecraft.createWorldOpenFlows().createFreshLevel(
				worldName,
				levelSettings,
				worldOptions,
				WorldPresets::createNormalWorldDimensions,
				new TitleScreen()
		);
	}
}
