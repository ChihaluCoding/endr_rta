package chihalu.endrrta.client.reset;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

public final class SeedResetWorldStarter {
	private static final DateTimeFormatter WORLD_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

	private SeedResetWorldStarter() {
	}

	public static void startNewWorld(Minecraft minecraft) {
		String worldName = "EndrRTA-Reset-" + LocalDateTime.now().format(WORLD_NAME_FORMAT)
				+ "-" + UUID.randomUUID().toString().substring(0, 8);
		LevelSettings levelSettings = new LevelSettings(
				worldName,
				GameType.SURVIVAL,
				LevelSettings.DifficultySettings.DEFAULT,
				false,
				WorldDataConfiguration.DEFAULT
		);

		minecraft.createWorldOpenFlows().createFreshLevel(
				worldName,
				levelSettings,
				WorldOptions.defaultWithRandomSeed(),
				WorldPresets::createNormalWorldDimensions,
				new TitleScreen()
		);
	}
}
