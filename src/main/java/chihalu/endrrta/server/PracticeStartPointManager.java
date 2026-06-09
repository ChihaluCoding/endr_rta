package chihalu.endrrta.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfigManager;

public final class PracticeStartPointManager {
	private static final Map<String, PracticeDestination> START_POINTS = new HashMap<>();
	private static final Map<UUID, String> SELECTED_SCENARIOS = new HashMap<>();
	private static final List<String> BASTION_RANDOM_CANDIDATES = List.of(
			"bastion_housing",
			"bastion_bridge",
			"bastion_treasure",
			"bastion_hoglin_stables"
	);

	private PracticeStartPointManager() {
	}

	public static void clear() {
		START_POINTS.clear();
		SELECTED_SCENARIOS.clear();
	}

	public static Map<String, PracticeStartPointSnapshot> snapshot() {
		Map<String, PracticeStartPointSnapshot> snapshot = new HashMap<>();
		for (Map.Entry<String, PracticeDestination> entry : START_POINTS.entrySet()) {
			PracticeDestination destination = entry.getValue();
			PracticeStartPointSnapshot point = new PracticeStartPointSnapshot();
			point.dimension = destination.dimension().identifier().toString();
			point.x = destination.pos().getX();
			point.y = destination.pos().getY();
			point.z = destination.pos().getZ();
			snapshot.put(entry.getKey(), point);
		}
		return snapshot;
	}

	public static void restore(Map<String, PracticeStartPointSnapshot> snapshot) {
		START_POINTS.clear();
		for (Map.Entry<String, PracticeStartPointSnapshot> entry : snapshot.entrySet()) {
			String scenarioId = entry.getKey();
			PracticeStartPointSnapshot point = entry.getValue();
			@Nullable ResourceKey<Level> dimension = parseDimension(point.dimension);
			if (dimension != null && !"none".equals(scenarioId)) {
				START_POINTS.put(scenarioId, new PracticeDestination(scenarioId, label(scenarioId), dimension, new BlockPos(point.x, point.y, point.z)));
			}
		}
	}

	public static Map<String, PracticeDestination> destinations() {
		return Map.copyOf(START_POINTS);
	}

	public static @Nullable PracticeDestination destination(String scenarioId) {
		return START_POINTS.get(scenarioId);
	}

	public static @Nullable PracticeDestination randomBastionDestination() {
		List<PracticeDestination> candidates = BASTION_RANDOM_CANDIDATES.stream()
				.map(START_POINTS::get)
				.filter(java.util.Objects::nonNull)
				.toList();
		if (candidates.isEmpty()) {
			return null;
		}
		return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
	}

	public static void selectScenario(ServerPlayer player, String scenarioId) {
		SELECTED_SCENARIOS.put(player.getUUID(), scenarioId);
	}

	public static @Nullable String selectedScenario(ServerPlayer player) {
		String selected = SELECTED_SCENARIOS.get(player.getUUID());
		if (selected != null) {
			return selected;
		}
		String practiceScenario = EndrRTAConfigManager.get().practiceScenario;
		if (practiceScenario != null && !"none".equals(practiceScenario)) {
			return practiceScenario;
		}
		return null;
	}

	public static PracticeDestination setSelectedStartPoint(ServerPlayer player, BlockPos pos, ResourceKey<Level> dimension) {
		String scenarioId = selectedScenario(player);
		if (scenarioId == null) {
			throw new IllegalStateException("先に練習メニューで項目を選んでください。");
		}
		return setStartPoint(scenarioId, pos, dimension);
	}

	public static PracticeDestination setStartPoint(String scenarioId, BlockPos pos, ResourceKey<Level> dimension) {
		PracticeDestination destination = new PracticeDestination(scenarioId, label(scenarioId), dimension, pos);
		START_POINTS.put(scenarioId, destination);
		return destination;
	}

	public static @Nullable String scenarioId(String value) {
		return switch (value.trim()) {
			case "nether_fortress", "fortress", "ネザー要塞" -> "nether_fortress";
			case "bastion", "ピグリン要塞" -> "bastion";
			case "housing", "bastion_housing", "住居" -> "bastion_housing";
			case "bridge", "bastion_bridge", "橋" -> "bastion_bridge";
			case "treasure", "bastion_treasure", "宝箱部屋" -> "bastion_treasure";
			case "hoglin", "hoglin_stables", "bastion_hoglin_stables", "ホグリンの部屋" -> "bastion_hoglin_stables";
			case "warped_forest_pearls", "pearls", "歪んだ森エンパ", "エンパ" -> "warped_forest_pearls";
			case "lava_pool_portal", "portal", "溶岩ゲート", "マグマ溜まりゲート" -> "lava_pool_portal";
			case "ender_dragon", "dragon", "エンドラ討伐", "エンドラ" -> "ender_dragon";
			default -> null;
		};
	}

	public static String label(String scenarioId) {
		return switch (scenarioId) {
			case "nether_fortress" -> "ネザー要塞";
			case "bastion" -> "ピグリン要塞";
			case "bastion_housing" -> "住居";
			case "bastion_bridge" -> "橋";
			case "bastion_treasure" -> "宝箱部屋";
			case "bastion_hoglin_stables" -> "ホグリンの部屋";
			case "warped_forest_pearls" -> "歪んだ森エンパ";
			case "lava_pool_portal" -> "マグマ溜まりゲート";
			case "ender_dragon" -> "エンドラ討伐";
			default -> "個別";
		};
	}

	private static @Nullable ResourceKey<Level> parseDimension(String dimensionId) {
		if (dimensionId == null || dimensionId.isBlank()) {
			return null;
		}
		try {
			return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
