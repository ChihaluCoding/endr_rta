package chihalu.endrrta.client.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import chihalu.endrrta.client.pie.PieChartAssistHandler;
import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import chihalu.endrrta.server.EndrRTAServerState;
import chihalu.endrrta.server.RadarTarget;
import chihalu.endrrta.server.RunState;
import chihalu.endrrta.server.SplitRecord;
import chihalu.endrrta.server.SplitType;

public final class EndrRTAHud {
	private static final int PANEL_TOP_RGB = 0x00121720;
	private static final int PANEL_BOTTOM_RGB = 0x0007090E;
	private static final int SHADOW = 0x77000000;
	private static final int BORDER = 0x553A4657;
	private static final int CARD = 0x4AFFFFFF;
	private static final int CARD_DARK = 0x33000000;
	private static final int DIVIDER = 0x26FFFFFF;
	private static final int TEXT = 0xFFF4F7FB;
	private static final int LABEL = 0xFF95A3B3;
	private static final int MUTED = 0xFFB5C0CB;
	private static final int ACCENT = 0xFFFFD166;
	private static final int SUCCESS = 0xFF7EE787;
	private static final int INFO = 0xFF7CC7FF;
	private static final int WARNING = 0xFFFF9F43;
	private static final int DANGER = 0xFFFF6B6B;
	private static final int PRACTICE_RAIL = 0xFF32D583;
	private static final int COMPETITION_RAIL = 0xFFFF9F43;
	private static final int PANEL_WIDTH = 224;
	private static final int TIMER_CARD_HEIGHT = 24;
	private static final int ROW_HEIGHT = 12;
	private static final int PANEL_PADDING = 10;
	private static final long BIOME_NAME_SWITCH_MILLIS = 3_000L;
	private static final Map<String, String> JAPANESE_BIOME_NAMES = createJapaneseBiomeNames();
	private static int cachedPanelOpacity = -1;
	private static int cachedPanelTop = PANEL_TOP_RGB;
	private static int cachedPanelBottom = PANEL_BOTTOM_RGB;

	private EndrRTAHud() {
	}

	public static void render(@NonNull GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		EndrRTAConfig config = EndrRTAConfigManager.get();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (!shouldRender(minecraft, config, player, level)) {
			return;
		}

		HudContent content = buildContent(minecraft, Objects.requireNonNull(player), Objects.requireNonNull(level), config);
		Font font = minecraft.font;
		int maxHeight = Math.max(96, graphics.guiHeight() / 2);
		int availableRows = Math.max(1, (maxHeight - 66) / ROW_HEIGHT);
		List<@NonNull HudMetric> rows = compactRows(content, availableRows);
		int height = Math.min(maxHeight, 66 + rows.size() * ROW_HEIGHT);
		int x = 8;
		int y = 8;
		int opacity = Math.clamp(config.hudBackgroundOpacity, 0, 100);

		if (opacity > 0) {
			updatePanelColors(opacity);
			graphics.fill(x + 3, y + 3, x + PANEL_WIDTH + 3, y + height + 3, SHADOW);
			graphics.fillGradient(x, y, x + PANEL_WIDTH, y + height, cachedPanelTop, cachedPanelBottom);
		}
		graphics.outline(x, y, PANEL_WIDTH, height, BORDER);
		graphics.fill(x, y, x + 4, y + height, content.railColor());

		drawHeader(graphics, font, content, x, y);
		drawTimers(graphics, font, content, x, y + 26);
		drawRows(graphics, font, rows, x, y + 56);
	}

	private static boolean shouldRender(Minecraft minecraft, EndrRTAConfig config, @Nullable LocalPlayer player, @Nullable ClientLevel level) {
		if (!config.showHud || player == null || level == null || minecraft.options.hideGui) {
			return false;
		}
		return !minecraft.getDebugOverlay().showDebugScreen() && !minecraft.getDebugOverlay().showProfilerChart();
	}

	private static void updatePanelColors(int opacityPercent) {
		if (cachedPanelOpacity == opacityPercent) {
			return;
		}
		cachedPanelOpacity = opacityPercent;
		cachedPanelTop = panelColor(PANEL_TOP_RGB, opacityPercent);
		cachedPanelBottom = panelColor(PANEL_BOTTOM_RGB, opacityPercent);
	}

	private static int panelColor(int rgb, int opacityPercent) {
		int alpha = opacityPercent * 255 / 100;
		return (alpha << 24) | (rgb & 0x00FFFFFF);
	}

	private static void drawHeader(GuiGraphicsExtractor graphics, Font font, HudContent content, int x, int y) {
		graphics.text(font, "EndraRTA", x + 12, y + 7, ACCENT, true);
		graphics.text(font, content.mode(), x + PANEL_WIDTH - 12 - font.width(content.mode()), y + 7, content.modeColor(), true);
		graphics.fill(x + 12, y + 21, x + PANEL_WIDTH - 12, y + 22, DIVIDER);
	}

	private static void drawTimers(GuiGraphicsExtractor graphics, Font font, HudContent content, int x, int y) {
		int cardX = x + PANEL_PADDING;
		int cardWidth = PANEL_WIDTH - PANEL_PADDING * 2;
		graphics.fill(cardX, y, cardX + cardWidth, y + TIMER_CARD_HEIGHT, CARD_DARK);
		graphics.outline(cardX, y, cardWidth, TIMER_CARD_HEIGHT, CARD);
		graphics.fill(cardX, y, cardX + 3, y + TIMER_CARD_HEIGHT, SUCCESS);
		graphics.text(font, "RTA", cardX + 8, y + 4, LABEL, true);
		graphics.text(font, content.rta(), cardX + 42, y + 4, TEXT, true);
		graphics.text(font, "IGT", cardX + 8, y + 14, LABEL, true);
		graphics.text(font, content.igt(), cardX + 42, y + 14, INFO, true);
	}

	private static void drawRows(GuiGraphicsExtractor graphics, Font font, List<@NonNull HudMetric> rows, int x, int y) {
		for (int i = 0; i < rows.size(); i++) {
			HudMetric row = Objects.requireNonNull(rows.get(i), "HUD row");
			int rowY = y + i * ROW_HEIGHT;
			graphics.fill(x + PANEL_PADDING, rowY - 1, x + PANEL_WIDTH - PANEL_PADDING, rowY + 9, i % 2 == 0 ? 0x14000000 : 0x10FFFFFF);
			graphics.fill(x + PANEL_PADDING + 2, rowY + 3, x + PANEL_PADDING + 5, rowY + 6, row.color());
			graphics.text(font, row.label(), x + PANEL_PADDING + 10, rowY, LABEL, true);
			int valueX = x + PANEL_PADDING + 68;
			String value = fitText(font, row.value(), x + PANEL_WIDTH - PANEL_PADDING - valueX - 2);
			graphics.text(font, value, valueX, rowY, row.color(), true);
		}
	}

	private static List<@NonNull HudMetric> compactRows(HudContent content, int maxRows) {
		List<@NonNull HudMetric> rows = new ArrayList<>();
		rows.addAll(content.metrics());
		rows.addAll(content.alerts());
		if (rows.size() <= maxRows) {
			return rows;
		}

		int hiddenCount = rows.size() - maxRows + 1;
		List<@NonNull HudMetric> visibleRows = new ArrayList<>(rows.subList(0, Math.max(0, maxRows - 1)));
		visibleRows.add(new HudMetric("ほか", "+" + hiddenCount + "件", MUTED));
		return visibleRows;
	}

	private static String fitText(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		int suffixWidth = font.width(suffix);
		StringBuilder builder = new StringBuilder(text);
		while (!builder.isEmpty() && font.width(builder.toString()) + suffixWidth > maxWidth) {
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.isEmpty() ? suffix : builder + suffix;
	}

	private static HudContent buildContent(Minecraft minecraft, LocalPlayer player, ClientLevel level, EndrRTAConfig config) {
		RunState run = currentRun(minecraft, player);
		String rta = run == null ? "--:--.-" : formatMillis(run.elapsedRtaMillis());
		String igt = run == null ? "--:--.-" : formatTicks(run.igtTicks());
		List<@NonNull HudMetric> metrics = new ArrayList<>();
		List<@NonNull HudMetric> alerts = new ArrayList<>();

		SplitRecord latest = run == null ? null : run.latestSplit();
		metrics.add(latest == null
				? new HudMetric("最新", "なし", MUTED)
				: new HudMetric("最新", latest.type().label() + " " + formatMillis(latest.rtaMillis()), SUCCESS));

		BlockPos pos = Objects.requireNonNull(player.blockPosition(), "player block position");
		if (config.showCoordinateConversion) {
			metrics.add(convertedCoordinateMetric(level.dimension(), pos));
		}
		if (config.showBiome) {
			metrics.add(new HudMetric("バイオーム", biomeName(level, pos), MUTED));
		}
		if (config.showLightLevel) {
			metrics.add(new HudMetric("明るさ", String.valueOf(level.getMaxLocalRawBrightness(pos)), INFO));
		}
		if (config.showCrystalCount && level.dimension() == Level.END) {
			metrics.add(new HudMetric("クリスタル", String.valueOf(countEndCrystals(level)), ACCENT));
		}
		if (config.showBastionType && level.dimension() == Level.NETHER) {
			metrics.add(new HudMetric("ピグリン要塞", run == null ? "未検出" : run.bastionType(), WARNING));
		}
		if (config.allowsPracticeAssist() && config.showRadar) {
			if (level.dimension() == Level.OVERWORLD) {
				addRadarMetric(metrics, "エンド要塞", run == null ? null : run.stronghold());
			} else if (level.dimension() == Level.NETHER) {
				addRadarMetric(metrics, "ネザー要塞", run == null ? null : run.fortress());
			}
		}
		if (config.allowsPracticeAssist() && config.showBedBlastAssist && lookingAtBed(minecraft, level)) {
			alerts.add(new HudMetric("ベッド", "危険 r5 / 安全 r7+", WARNING));
		}
		if (config.isCompetitionMode()) {
			alerts.add(new HudMetric("競技", "練習補助 OFF", DANGER));
		}
		if (PieChartAssistHandler.shouldShowHint(minecraft)) {
			alerts.add(new HudMetric("円グラフ", PieChartAssistHandler.selectedLabel(minecraft), ACCENT));
			alerts.add(new HudMetric("操作", "ホイール:選択 左:決定 右:戻る", MUTED));
		}

		String mode = config.practiceMode ? "練習モード" : "競技モード";
		int modeColor = config.practiceMode ? SUCCESS : WARNING;
		int railColor = config.practiceMode ? PRACTICE_RAIL : COMPETITION_RAIL;
		return new HudContent(mode, modeColor, railColor, rta, igt, List.copyOf(metrics), List.copyOf(alerts));
	}

	private static @Nullable RunState currentRun(Minecraft minecraft, LocalPlayer player) {
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server == null) {
			return null;
		}
		ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
		return serverPlayer == null ? null : EndrRTAServerState.getRun(serverPlayer.getUUID());
	}

	private static HudMetric convertedCoordinateMetric(ResourceKey<Level> dimension, @NonNull BlockPos pos) {
		if (dimension == Level.NETHER) {
			return new HudMetric("地上座標", pos.getX() * 8 + ", " + pos.getZ() * 8, TEXT);
		}
		if (dimension == Level.OVERWORLD) {
			return new HudMetric("ネザー座標", Math.floorDiv(pos.getX(), 8) + ", " + Math.floorDiv(pos.getZ(), 8), TEXT);
		}
		return new HudMetric("座標", pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), TEXT);
	}

	private static String biomeName(ClientLevel level, @NonNull BlockPos pos) {
		Identifier id = level.getBiome(pos)
				.unwrapKey()
				.map(ResourceKey::identifier)
				.orElseGet(() -> {
					Identifier fallbackId = level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(level.getBiome(pos).value());
					return fallbackId == null ? Identifier.withDefaultNamespace("unknown") : fallbackId;
				});
		String path = biomePath(id);
		return shouldShowJapaneseBiomeName() ? JAPANESE_BIOME_NAMES.getOrDefault(path, readableIdentifier(id)) : readableIdentifier(id);
	}

	private static String readableIdentifier(Identifier id) {
		String[] words = biomePath(id).split("_");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				builder.append(word.substring(1));
			}
		}
		return builder.isEmpty() ? "Unknown" : builder.toString();
	}

	private static boolean shouldShowJapaneseBiomeName() {
		return (System.currentTimeMillis() / BIOME_NAME_SWITCH_MILLIS) % 2L == 0L;
	}

	private static String biomePath(Identifier id) {
		String value = id.toString();
		int namespaceEnd = value.indexOf(':');
		return namespaceEnd >= 0 ? value.substring(namespaceEnd + 1) : value;
	}

	private static Map<String, String> createJapaneseBiomeNames() {
		Map<String, String> names = new HashMap<>();
		names.put("badlands", "荒野");
		names.put("bamboo_jungle", "竹林");
		names.put("basalt_deltas", "玄武岩の三角州");
		names.put("beach", "砂浜");
		names.put("birch_forest", "シラカバの森");
		names.put("cherry_grove", "サクラの林");
		names.put("cold_ocean", "冷たい海");
		names.put("crimson_forest", "真紅の森");
		names.put("dark_forest", "暗い森");
		names.put("deep_cold_ocean", "冷たい深海");
		names.put("deep_dark", "ディープダーク");
		names.put("deep_frozen_ocean", "凍った深海");
		names.put("deep_lukewarm_ocean", "ぬるい深海");
		names.put("deep_ocean", "深海");
		names.put("desert", "砂漠");
		names.put("dripstone_caves", "鍾乳洞");
		names.put("end_barrens", "ジ・エンドのやせ地");
		names.put("end_highlands", "ジ・エンドの高地");
		names.put("end_midlands", "ジ・エンドの内陸部");
		names.put("eroded_badlands", "侵食された荒野");
		names.put("flower_forest", "花の森");
		names.put("forest", "森林");
		names.put("frozen_ocean", "凍った海");
		names.put("frozen_peaks", "凍った山頂");
		names.put("frozen_river", "凍った川");
		names.put("grove", "林");
		names.put("ice_spikes", "氷樹");
		names.put("jagged_peaks", "尖った山頂");
		names.put("jungle", "ジャングル");
		names.put("lukewarm_ocean", "ぬるい海");
		names.put("lush_caves", "繁茂した洞窟");
		names.put("mangrove_swamp", "マングローブの沼地");
		names.put("meadow", "草地");
		names.put("mushroom_fields", "キノコ島");
		names.put("nether_wastes", "ネザーの荒地");
		names.put("old_growth_birch_forest", "シラカバの原生林");
		names.put("old_growth_pine_taiga", "マツの原生タイガ");
		names.put("old_growth_spruce_taiga", "トウヒの原生タイガ");
		names.put("ocean", "海");
		names.put("pale_garden", "ペールガーデン");
		names.put("plains", "平原");
		names.put("river", "川");
		names.put("savanna", "サバンナ");
		names.put("savanna_plateau", "サバンナの高原");
		names.put("small_end_islands", "小さなエンド島");
		names.put("snowy_beach", "雪の砂浜");
		names.put("snowy_plains", "雪原");
		names.put("snowy_slopes", "雪の斜面");
		names.put("snowy_taiga", "雪のタイガ");
		names.put("soul_sand_valley", "ソウルサンドの谷");
		names.put("sparse_jungle", "まばらなジャングル");
		names.put("stony_peaks", "石だらけの山頂");
		names.put("stony_shore", "石だらけの海岸");
		names.put("sunflower_plains", "ヒマワリ平原");
		names.put("swamp", "沼地");
		names.put("taiga", "タイガ");
		names.put("the_end", "ジ・エンド");
		names.put("the_void", "奈落");
		names.put("warm_ocean", "暖かい海");
		names.put("warped_forest", "歪んだ森");
		names.put("windswept_forest", "吹きさらしの森");
		names.put("windswept_gravelly_hills", "吹きさらしの砂利の丘");
		names.put("windswept_hills", "吹きさらしの丘");
		names.put("windswept_savanna", "吹きさらしのサバンナ");
		names.put("wooded_badlands", "森のある荒野");
		return Map.copyOf(names);
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

	private static void addRadarMetric(List<@NonNull HudMetric> metrics, String label, @Nullable RadarTarget target) {
		if (target == null) {
			metrics.add(new HudMetric(label, "????", MUTED));
			return;
		}
		metrics.add(new HudMetric(label, "X%d Z%d %dm".formatted(
				target.pos().getX(),
				target.pos().getZ(),
				(int) Math.round(target.distance())
		), SUCCESS));
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

	private record HudContent(String mode, int modeColor, int railColor, String rta, String igt,
			List<@NonNull HudMetric> metrics, List<@NonNull HudMetric> alerts) {
	}

	private record HudMetric(String label, String value, int color) {
	}
}
