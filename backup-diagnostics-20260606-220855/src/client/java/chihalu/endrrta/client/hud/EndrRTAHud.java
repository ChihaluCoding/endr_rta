package chihalu.endrrta.client.hud;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import chihalu.endrrta.server.EndrRTAServerState;
import chihalu.endrrta.server.RadarTarget;
import chihalu.endrrta.server.RunState;
import chihalu.endrrta.server.SplitRecord;

public final class EndrRTAHud {
	private static final int BACKGROUND = 0x66000000;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int ACCENT = 0xFFFFD166;

	private EndrRTAHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		EndrRTAConfig config = EndrRTAConfigManager.get();
		if (!config.showHud || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
			return;
		}

		List<String> lines = buildLines(minecraft, config);
		int width = lines.stream().mapToInt(line -> minecraft.font.width(line)).max().orElse(0) + 10;
		int height = lines.size() * 10 + 6;
		graphics.fill(4, 4, 4 + width, 4 + height, BACKGROUND);
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(minecraft.font, lines.get(i), 9, 8 + i * 10, i == 0 ? ACCENT : TEXT, true);
		}
	}

	private static List<String> buildLines(Minecraft minecraft, EndrRTAConfig config) {
		List<String> lines = new ArrayList<>();
		RunState run = currentRun(minecraft);
		lines.add("EndrRTA " + (config.practiceMode ? "練習" : "競技"));
		if (run != null) {
			lines.add("RTA " + formatMillis(run.elapsedRtaMillis()) + " / IGT " + formatTicks(run.igtTicks()));
			SplitRecord latest = run.latestSplit();
			lines.add(latest == null ? "Split -" : "Split " + latest.type().label() + " " + formatMillis(latest.rtaMillis()));
		} else {
			lines.add("RTA - / IGT -");
			lines.add("Split -");
		}

		BlockPos pos = minecraft.player.blockPosition();
		if (config.showCoordinateConversion) {
			lines.add(convertedCoordinateLine(minecraft.level.dimension(), pos));
		}
		if (config.showBiome) {
			lines.add("Biome " + biomeName(minecraft, pos));
		}
		if (config.showLightLevel) {
			lines.add("Light " + minecraft.level.getMaxLocalRawBrightness(pos));
		}
		if (config.showCrystalCount && minecraft.level.dimension() == Level.END) {
			lines.add("Crystal " + countEndCrystals(minecraft));
		}
		if (config.allowsPracticeAssist() && config.showRadar) {
			addRadarLine(lines, "Stronghold", run == null ? null : run.stronghold(), pos);
			addRadarLine(lines, "Fortress", run == null ? null : run.fortress(), pos);
		}
		if (config.allowsPracticeAssist() && config.showBedBlastAssist && lookingAtBed(minecraft)) {
			lines.add("Bed danger r5 / safe r7+");
		}
		if (config.isCompetitionMode()) {
			lines.add("競技モード: 補助OFF");
		}
		return lines;
	}

	private static RunState currentRun(Minecraft minecraft) {
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server == null || minecraft.player == null) {
			return null;
		}
		ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
		return serverPlayer == null ? null : EndrRTAServerState.getRun(serverPlayer.getUUID());
	}

	private static String convertedCoordinateLine(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
		if (dimension == Level.NETHER) {
			return "OW " + pos.getX() * 8 + ", " + pos.getZ() * 8;
		}
		if (dimension == Level.OVERWORLD) {
			return "Nether " + Math.floorDiv(pos.getX(), 8) + ", " + Math.floorDiv(pos.getZ(), 8);
		}
		return "Coord " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	private static String biomeName(Minecraft minecraft, BlockPos pos) {
		return minecraft.level.getBiome(pos)
				.unwrapKey()
				.map(key -> key.identifier().toString())
				.orElseGet(() -> {
					Identifier id = minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(minecraft.level.getBiome(pos).value());
					return id == null ? "unknown" : id.toString();
				});
	}

	private static int countEndCrystals(Minecraft minecraft) {
		int count = 0;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity.getType() == EntityType.END_CRYSTAL && entity.isAlive()) {
				count++;
			}
		}
		return count;
	}

	private static void addRadarLine(List<String> lines, String label, RadarTarget target, BlockPos playerPos) {
		if (target == null) {
			lines.add(label + " -");
			return;
		}
		int dx = target.pos().getX() - playerPos.getX();
		int dz = target.pos().getZ() - playerPos.getZ();
		lines.add(label + " " + direction(dx, dz) + " " + Math.round(target.distance()) + "m");
	}

	private static String direction(int dx, int dz) {
		double angle = Math.toDegrees(Math.atan2(-dx, dz));
		if (angle < 0.0D) {
			angle += 360.0D;
		}
		String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		return names[(int) Math.round(angle / 45.0D) % names.length];
	}

	private static boolean lookingAtBed(Minecraft minecraft) {
		if (!(minecraft.hitResult instanceof BlockHitResult blockHit) || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		return minecraft.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof BedBlock;
	}

	private static String formatTicks(long ticks) {
		return formatMillis(ticks * 50L);
	}

	private static String formatMillis(long millis) {
		long totalSeconds = millis / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		long tenths = (millis % 1000L) / 100L;
		return "%02d:%02d.%d".formatted(minutes, seconds, tenths);
	}
}
