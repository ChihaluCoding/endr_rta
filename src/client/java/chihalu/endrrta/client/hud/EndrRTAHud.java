package chihalu.endrrta.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import chihalu.endrrta.server.EndrRTAServerState;
import chihalu.endrrta.server.RadarTarget;
import chihalu.endrrta.server.RunState;
import chihalu.endrrta.server.SplitRecord;

public final class EndrRTAHud {
	private static final int PANEL_TOP = 0xE00B1018;
	private static final int PANEL_BOTTOM = 0xD006090E;
	private static final int PANEL_SHADOW = 0x66000000;
	private static final int BORDER = 0x668FA1B3;
	private static final int INNER_BORDER = 0x22000000;
	private static final int DIVIDER = 0x33FFFFFF;
	private static final int ROW_SHADE = 0x18000000;
	private static final int TEXT = 0xFFF4F7FB;
	private static final int LABEL = 0xFF91A0AE;
	private static final int MUTED = 0xFFB8C3CF;
	private static final int ACCENT = 0xFFFFD166;
	private static final int SUCCESS = 0xFF7EE787;
	private static final int WARNING = 0xFFFF9F43;
	private static final int PRACTICE_BADGE = 0x5532D583;
	private static final int COMPETITION_BADGE = 0x55FF9F43;
	private static final int MIN_WIDTH = 206;
	private static final int PADDING_X = 10;
	private static final int ROW_HEIGHT = 10;

	private EndrRTAHud() {
	}

	public static void render(@NonNull GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		EndrRTAConfig config = EndrRTAConfigManager.get();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (!config.showHud || player == null || level == null || minecraft.options.hideGui) {
			return;
		}

		HudContent content = buildContent(minecraft, player, level, config);
		Font font = minecraft.font;
		int width = calculateWidth(font, content);
		int x = 6;
		int y = 6;
		int rowsY = y + 39;
		int height = rowsY - y + content.rows().size() * ROW_HEIGHT + 8;

		graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, PANEL_SHADOW);
		graphics.fillGradient(x, y, x + width, y + height, PANEL_TOP, PANEL_BOTTOM);
		graphics.outline(x, y, width, height, BORDER);
		graphics.outline(x + 1, y + 1, width - 2, height - 2, INNER_BORDER);
		graphics.fill(x, y, x + width, y + 2, ACCENT);

		drawHeader(graphics, font, content, x, y, width);
		drawTimers(graphics, font, content, x, y, width);
		drawRows(graphics, font, content.rows(), x, rowsY, width);
	}

	private static int calculateWidth(Font font, HudContent content) {
		int headerWidth = font.width("EndrRTA") + 22 + font.width(content.mode()) + 10;
		int timerWidth = font.width("RTA") + 4 + font.width(content.rta())
				+ 20 + font.width("IGT") + 4 + font.width(content.igt());
		int labelWidth = content.rows().stream().mapToInt(row -> font.width(row.label())).max().orElse(0);
		int valueWidth = content.rows().stream().mapToInt(row -> font.width(row.value())).max().orElse(0);
		int rowWidth = labelWidth + 12 + valueWidth;
		return Math.max(MIN_WIDTH, Math.max(Math.max(headerWidth, timerWidth), rowWidth) + PADDING_X * 2);
	}

	private static void drawHeader(GuiGraphicsExtractor graphics, Font font, HudContent content, int x, int y, int width) {
		int modeWidth = font.width(content.mode()) + 10;
		int modeX = x + width - PADDING_X - modeWidth;
		graphics.text(font, "EndrRTA", x + PADDING_X, y + 7, ACCENT, true);
		graphics.fill(modeX, y + 5, modeX + modeWidth, y + 17, content.modeBackground());
		graphics.text(font, content.mode(), modeX + 5, y + 7, content.modeColor(), true);
		graphics.fill(x + PADDING_X, y + 21, x + width - PADDING_X, y + 22, DIVIDER);
	}

	private static void drawTimers(GuiGraphicsExtractor graphics, Font font, HudContent content, int x, int y, int width) {
		int timerY = y + 26;
		int igtLabelWidth = font.width("IGT");
		int igtValueWidth = font.width(content.igt());
		int igtLabelX = x + width - PADDING_X - igtLabelWidth - 4 - igtValueWidth;

		graphics.text(font, "RTA", x + PADDING_X, timerY, LABEL, true);
		graphics.text(font, content.rta(), x + PADDING_X + font.width("RTA") + 4, timerY, TEXT, true);
		graphics.text(font, "IGT", igtLabelX, timerY, LABEL, true);
		graphics.text(font, content.igt(), igtLabelX + igtLabelWidth + 4, timerY, TEXT, true);
	}

	private static void drawRows(GuiGraphicsExtractor graphics, Font font, List<@NonNull HudRow> rows, int x, int y, int width) {
		int labelWidth = rows.stream().mapToInt(row -> font.width(row.label())).max().orElse(0);
		int valueX = x + PADDING_X + labelWidth + 12;
		for (int i = 0; i < rows.size(); i++) {
			HudRow row = Objects.requireNonNull(rows.get(i), "HUD row");
			int rowY = y + i * ROW_HEIGHT;
			if (i % 2 == 1) {
				graphics.fill(x + 6, rowY - 1, x + width - 6, rowY + 9, ROW_SHADE);
			}
			graphics.text(font, row.label(), x + PADDING_X, rowY, LABEL, true);
			graphics.text(font, row.value(), valueX, rowY, row.valueColor(), true);
		}
	}

	private static HudContent buildContent(Minecraft minecraft, LocalPlayer player, ClientLevel level, EndrRTAConfig config) {
		List<@NonNull HudRow> rows = new ArrayList<>();
		RunState run = currentRun(minecraft, player);
		String rta = run == null ? "--:--.-" : formatMillis(run.elapsedRtaMillis());
		String igt = run == null ? "--:--.-" : formatTicks(run.igtTicks());
		SplitRecord latest = run == null ? null : run.latestSplit();
		rows.add(latest == null
				? new HudRow("最新", "なし", MUTED)
				: new HudRow("最新", latest.type().label() + "  " + formatMillis(latest.rtaMillis()), SUCCESS));

		BlockPos pos = Objects.requireNonNull(player.blockPosition(), "player block position");
		if (config.showCoordinateConversion) {
			rows.add(convertedCoordinateRow(level.dimension(), pos));
		}
		if (config.showBiome) {
			rows.add(new HudRow("バイオーム", biomeName(level, pos), MUTED));
		}
		if (config.showLightLevel) {
			rows.add(new HudRow("明るさ", String.valueOf(level.getMaxLocalRawBrightness(pos)), MUTED));
		}
		if (config.showCrystalCount && level.dimension() == Level.END) {
			rows.add(new HudRow("クリスタル", String.valueOf(countEndCrystals(level)), ACCENT));
		}
		if (config.allowsPracticeAssist() && config.showRadar) {
			addRadarRow(rows, "エンド要塞", run == null ? null : run.stronghold(), pos);
			addRadarRow(rows, "ネザー要塞", run == null ? null : run.fortress(), pos);
		}
		if (config.allowsPracticeAssist() && config.showBedBlastAssist && lookingAtBed(minecraft, level)) {
			rows.add(new HudRow("ベッド", "危険 r5 / 安全 r7+", WARNING));
		}
		if (config.isCompetitionMode()) {
			rows.add(new HudRow("競技", "練習補助 OFF", WARNING));
		}

		String mode = config.practiceMode ? "練習" : "競技";
		int modeBackground = config.practiceMode ? PRACTICE_BADGE : COMPETITION_BADGE;
		int modeColor = config.practiceMode ? SUCCESS : WARNING;
		return new HudContent(mode, modeColor, modeBackground, rta, igt, List.copyOf(rows));
	}

	private static @Nullable RunState currentRun(Minecraft minecraft, LocalPlayer player) {
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server == null) {
			return null;
		}
		ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
		return serverPlayer == null ? null : EndrRTAServerState.getRun(serverPlayer.getUUID());
	}

	private static HudRow convertedCoordinateRow(ResourceKey<Level> dimension, @NonNull BlockPos pos) {
		if (dimension == Level.NETHER) {
			return new HudRow("地上座標", pos.getX() * 8 + ", " + pos.getZ() * 8, TEXT);
		}
		if (dimension == Level.OVERWORLD) {
			return new HudRow("ネザー座標", Math.floorDiv(pos.getX(), 8) + ", " + Math.floorDiv(pos.getZ(), 8), TEXT);
		}
		return new HudRow("座標", pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), TEXT);
	}

	private static String biomeName(ClientLevel level, @NonNull BlockPos pos) {
		return level.getBiome(pos)
				.unwrapKey()
				.map(key -> readableIdentifier(key.identifier()))
				.orElseGet(() -> {
					Identifier id = level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
					return id == null ? "不明" : readableIdentifier(id);
				});
	}

	private static String readableIdentifier(Identifier id) {
		String value = id.toString();
		int namespaceEnd = value.indexOf(':');
		String path = namespaceEnd >= 0 ? value.substring(namespaceEnd + 1) : value;
		return path.replace('_', ' ');
	}

	private static int countEndCrystals(ClientLevel level) {
		int count = 0;
		for (Entity entity : level.entitiesForRendering()) {
			if (entity.getType() == EntityType.END_CRYSTAL && entity.isAlive()) {
				count++;
			}
		}
		return count;
	}

	private static void addRadarRow(List<@NonNull HudRow> rows, String label, @Nullable RadarTarget target, @NonNull BlockPos playerPos) {
		if (target == null) {
			rows.add(new HudRow(label, "未検出", MUTED));
			return;
		}
		int dx = target.pos().getX() - playerPos.getX();
		int dz = target.pos().getZ() - playerPos.getZ();
		rows.add(new HudRow(label, direction(dx, dz) + "  " + Math.round(target.distance()) + "m", SUCCESS));
	}

	private static String direction(int dx, int dz) {
		double angle = Math.toDegrees(Math.atan2(-dx, dz));
		if (angle < 0.0D) {
			angle += 360.0D;
		}
		String[] names = {"北", "北東", "東", "南東", "南", "南西", "西", "北西"};
		return names[(int) Math.round(angle / 45.0D) % names.length];
	}

	private static boolean lookingAtBed(Minecraft minecraft, ClientLevel level) {
		HitResult hitResult = minecraft.hitResult;
		if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		BlockPos blockPos = Objects.requireNonNull(blockHit.getBlockPos(), "hit block position");
		return level.getBlockState(blockPos).getBlock() instanceof BedBlock;
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

	private record HudContent(String mode, int modeColor, int modeBackground, String rta, String igt,
			List<@NonNull HudRow> rows) {
	}

	private record HudRow(String label, String value, int valueColor) {
	}
}
