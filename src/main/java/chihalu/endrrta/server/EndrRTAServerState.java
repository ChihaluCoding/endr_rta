package chihalu.endrrta.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.EndrRTA;
import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class EndrRTAServerState {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final @NonNull Set<@NonNull Relative> ABSOLUTE_TELEPORT = Set.of();
	private static final Map<UUID, RunState> RUNS = new HashMap<>();
	private static final Map<UUID, BlockPos> OVERWORLD_RETURN_POSITIONS = new HashMap<>();
	private static final Map<UUID, BlockPos> FORTRESS_WARPED_PORTALS = new HashMap<>();
	private static final Set<UUID> FORTRESS_WARPED_PORTAL_FAILED = new HashSet<>();
	private static final Set<UUID> VILLAGE_SPAWNED = new HashSet<>();
	private static final Set<UUID> PRACTICE_START_CHEST_PLACED = new HashSet<>();
	private static final String STATE_FILE_NAME = "endrrta-state.json";
	private static final int MIN_SAFE_SURFACE_OFFSET = 5;
	private static final int BASTION_START_SCAN_RADIUS_CHUNKS = 12;
	private static final int FORTRESS_PORTAL_STRUCTURE_SEARCH_RADIUS = 4_096;
	private static final int FORTRESS_WARPED_PAIR_SEARCH_RADIUS = 4_096;
	private static final int FORTRESS_WARPED_PAIR_SEARCH_STEP = 512;
	private static final int WARPED_FOREST_SEARCH_RADIUS = 1_024;
	private static final int WARPED_FOREST_MAX_DISTANCE = 1_024;
	private static final int PORTAL_SAFE_POS_SEARCH_RADIUS = 32;
	private static final int OVERWORLD_PORTAL_SCAN_RADIUS = 6;
	private static final int GENERATED_PORTAL_TRIGGER_RADIUS = 6;
	private static final int PRACTICE_START_CHEST_SEARCH_RADIUS = 5;
	private static final int ENDERMAN_BOAT_ASSIST_RADIUS = 32;
	private static final int ENDERMAN_BOAT_BOARD_DISTANCE = 3;
	private static final double ENDERMAN_BOAT_NAVIGATION_SPEED = 1.25D;
	private static final Identifier WARPED_FOREST_ID = Identifier.withDefaultNamespace("warped_forest");
	private static final String BASTION_UNKNOWN = "未検出";
	private static int radarTick;

	private EndrRTAServerState() {
	}

	public static void tickServer(MinecraftServer server) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		radarTick++;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RunState run = RUNS.computeIfAbsent(player.getUUID(), ignored -> new RunState());
			rememberOverworldReturnPosition(player);
			prepareVillageSpawn(player, config);
			placePracticeStartChest(player, config);
			handleFortressWarpedPortalReturn(player, config);
			assistEndermenIntoBoats(player, config);
			handleTimer(player, run, config);
			if (radarTick % 20 == 0) {
				updateRadar(player, run, config);
			}
		}
	}

	public static void prepareInitialSpawn(ServerPlayer player) {
		prepareVillageSpawn(player, EndrRTAConfigManager.get());
	}

	public static @Nullable RunState getRun(UUID playerId) {
		return RUNS.get(playerId);
	}

	public static void clear() {
		RUNS.clear();
		OVERWORLD_RETURN_POSITIONS.clear();
		FORTRESS_WARPED_PORTALS.clear();
		FORTRESS_WARPED_PORTAL_FAILED.clear();
		VILLAGE_SPAWNED.clear();
		PRACTICE_START_CHEST_PLACED.clear();
		PracticeStartPointManager.clear();
		radarTick = 0;
	}

	public static void load(MinecraftServer server) {
		Path path = statePath(server);
		if (Files.notExists(path)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			@Nullable EndrRTAWorldStateSnapshot snapshot = readWorldStateSnapshot(reader);
			if (snapshot == null) {
				return;
			}
			RUNS.clear();
			OVERWORLD_RETURN_POSITIONS.clear();
			FORTRESS_WARPED_PORTALS.clear();
			FORTRESS_WARPED_PORTAL_FAILED.clear();
			VILLAGE_SPAWNED.clear();
			PRACTICE_START_CHEST_PLACED.clear();
			PracticeStartPointManager.clear();
			for (Map.Entry<String, RunStateSnapshot> entry : snapshot.runs.entrySet()) {
				try {
					RUNS.put(UUID.fromString(entry.getKey()), RunState.restore(entry.getValue()));
				} catch (IllegalArgumentException exception) {
					EndrRTA.LOGGER.warn("EnderRTA のラン状態に不正なプレイヤーUUIDが含まれていたためスキップしました: {}", entry.getKey());
				}
			}
			for (String playerId : snapshot.villageSpawnedPlayers) {
				try {
					VILLAGE_SPAWNED.add(UUID.fromString(playerId));
				} catch (IllegalArgumentException exception) {
					EndrRTA.LOGGER.warn("EnderRTA の村スポーン状態に不正なプレイヤーUUIDが含まれていたためスキップしました: {}", playerId);
				}
			}
			for (String playerId : snapshot.practiceStartChestPlayers) {
				try {
					PRACTICE_START_CHEST_PLACED.add(UUID.fromString(playerId));
				} catch (IllegalArgumentException exception) {
					EndrRTA.LOGGER.warn("EnderRTA の初期チェスト状態に不正なプレイヤーUUIDが含まれていたためスキップしました: {}", playerId);
				}
			}
			PracticeStartPointManager.restore(snapshot.practiceStartPoints);
		} catch (IOException | IllegalArgumentException exception) {
			EndrRTA.LOGGER.warn("EnderRTA のワールド状態を読み込めませんでした。", exception);
		}
	}

	public static void save(MinecraftServer server) {
		Path path = statePath(server);
		EndrRTAWorldStateSnapshot snapshot = new EndrRTAWorldStateSnapshot();
		for (Map.Entry<UUID, RunState> entry : RUNS.entrySet()) {
			snapshot.runs.put(entry.getKey().toString(), entry.getValue().snapshot());
		}
		for (UUID playerId : VILLAGE_SPAWNED) {
			snapshot.villageSpawnedPlayers.add(playerId.toString());
		}
		for (UUID playerId : PRACTICE_START_CHEST_PLACED) {
			snapshot.practiceStartChestPlayers.add(playerId.toString());
		}
		snapshot.practiceStartPoints.putAll(PracticeStartPointManager.snapshot());

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(snapshot, writer);
			}
		} catch (IOException exception) {
			EndrRTA.LOGGER.warn("EnderRTA のワールド状態を書き込めませんでした。", exception);
		}
	}

	private static @Nullable EndrRTAWorldStateSnapshot readWorldStateSnapshot(Reader reader) {
		JsonElement root = JsonParser.parseReader(reader);
		if (!root.isJsonObject()) {
			return null;
		}

		JsonObject object = root.getAsJsonObject();
		if (object.has("runs") || object.has("villageSpawnedPlayers")) {
			return GSON.fromJson(root, EndrRTAWorldStateSnapshot.class);
		}

		@Nullable Map<String, RunStateSnapshot> legacyRuns = GSON.fromJson(
				root,
				new TypeToken<Map<String, RunStateSnapshot>>() {
				}.getType()
		);
		if (legacyRuns == null) {
			return null;
		}

		EndrRTAWorldStateSnapshot snapshot = new EndrRTAWorldStateSnapshot();
		snapshot.runs.putAll(legacyRuns);
		return snapshot;
	}

	public static void stopForDragon(ServerLevel level) {
		boolean changed = false;
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			RunState run = RUNS.get(player.getUUID());
			if (run != null && run.stopAtDragon()) {
				changed = true;
				showClearTimeTitle(player, run.elapsedRtaMillis());
			}
		}
		if (changed) {
			save(level.getServer());
		}
	}

	private static void showClearTimeTitle(ServerPlayer player, long rtaMillis) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(formatClearTime(rtaMillis))));
	}

	private static String formatClearTime(long rtaMillis) {
		long totalSeconds = Math.max(0L, rtaMillis / 1000L);
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0L) {
			return "%d時間%02d分%02d秒".formatted(hours, minutes, seconds);
		}
		return "%d分%02d秒".formatted(minutes, seconds);
	}

	private static Path statePath(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(STATE_FILE_NAME);
	}

	private static void prepareVillageSpawn(ServerPlayer player, EndrRTAConfig config) {
		if (!config.allowsForceVillageSpawn() || player.level().dimension() != Level.OVERWORLD) {
			return;
		}
		if (!VILLAGE_SPAWNED.add(player.getUUID())) {
			return;
		}

		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		@Nullable BlockPos village = player.level().findNearestMapStructure(
				StructureTags.VILLAGE,
				playerPos,
				config.villageSearchRadius,
				false
		);
		if (village == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] 指定範囲内に村が見つかりませんでした。"));
			return;
		}

		@Nullable BlockPos safePos = safeSurfacePos(player.level(), village);
		if (safePos == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] 村の安全な地表を取得できなかったため、初期移動を中止しました。"));
			return;
		}

		player.teleportTo(
				player.level(),
				safePos.getX() + 0.5D,
				safePos.getY(),
				safePos.getZ() + 0.5D,
				ABSOLUTE_TELEPORT,
				player.getYRot(),
				player.getXRot(),
				false
		);
		player.sendSystemMessage(Component.literal("[EnderRTA] 最寄り村へスポーン補正しました。"));
	}

	private static @Nullable BlockPos safeSurfacePos(ServerLevel level, BlockPos target) {
		ChunkAccess chunk = level.getChunk(target.getX() >> 4, target.getZ() >> 4, ChunkStatus.FULL, true);
		int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX() & 15, target.getZ() & 15);
		if (surfaceY <= level.getMinY() + MIN_SAFE_SURFACE_OFFSET) {
			return null;
		}
		return new BlockPos(target.getX(), surfaceY + 1, target.getZ());
	}

	private static void placePracticeStartChest(ServerPlayer player, EndrRTAConfig config) {
		if (!config.allowsPlacePracticeStartChest() || player.level().dimension() != Level.OVERWORLD) {
			return;
		}
		if (!PRACTICE_START_CHEST_PLACED.add(player.getUUID())) {
			return;
		}

		@Nullable BlockPos chestPos = findPracticeStartChestPos(player.level(), player.blockPosition());
		if (chestPos == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] 初期チェストを置ける場所が見つかりませんでした。"));
			return;
		}

		player.level().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
		if (player.level().getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			chest.setItem(0, new ItemStack(Items.IRON_AXE));
			chest.setItem(1, new ItemStack(Items.IRON_PICKAXE));
			chest.setItem(2, new ItemStack(Items.WATER_BUCKET));
			chest.setItem(3, new ItemStack(Items.BREAD, 64));
			chest.setItem(4, new ItemStack(Items.BREAD, 35));
			chest.setItem(5, new ItemStack(Items.OBSIDIAN, 10));
			chest.setItem(6, new ItemStack(Items.FLINT_AND_STEEL));
			chest.setItem(7, new ItemStack(Items.STONE_SHOVEL));
			chest.setItem(8, new ItemStack(Items.IRON_INGOT, 30));
			chest.setItem(9, new ItemStack(Items.IRON_HELMET));
			chest.setItem(10, new ItemStack(Items.IRON_CHESTPLATE));
			chest.setItem(11, new ItemStack(Items.IRON_LEGGINGS));
			chest.setItem(12, new ItemStack(Items.IRON_BOOTS));
			chest.setItem(13, unbreakingShield(player.level()));
			chest.setItem(14, new ItemStack(Items.GOLD_INGOT, 64));
			chest.setItem(15, new ItemStack(Items.CRAFTING_TABLE));
			chest.setItem(16, new ItemStack(Items.OAK_LOG, 64));
			chest.setItem(17, infinityBow(player.level()));
			chest.setItem(18, new ItemStack(Items.ARROW, 64));
			chest.setItem(19, new ItemStack(Items.GOLDEN_BOOTS));
			chest.setItem(20, new ItemStack(Items.ANVIL));
			chest.setChanged();
			player.sendSystemMessage(Component.literal("[EnderRTA] 練習用の初期チェストを配置しました。"));
		}
	}

	private static ItemStack unbreakingShield(ServerLevel level) {
		ItemStack shield = new ItemStack(Items.SHIELD);
		Holder<Enchantment> unbreaking = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.UNBREAKING);
		shield.enchant(unbreaking, 3);
		return shield;
	}

	private static ItemStack infinityBow(ServerLevel level) {
		ItemStack bow = new ItemStack(Items.BOW);
		Holder<Enchantment> infinity = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.INFINITY);
		bow.enchant(infinity, 1);
		return bow;
	}

	private static @Nullable BlockPos findPracticeStartChestPos(ServerLevel level, BlockPos playerPos) {
		for (int radius = 1; radius <= PRACTICE_START_CHEST_SEARCH_RADIUS; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
						continue;
					}
					BlockPos candidate = playerPos.offset(dx, 0, dz);
					if (canPlacePracticeStartChest(level, candidate)) {
						return candidate;
					}
				}
			}
		}
		return null;
	}

	private static boolean canPlacePracticeStartChest(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos.below()).isAir()
				&& level.getBlockState(pos).isAir()
				&& level.getBlockState(pos.above()).isAir();
	}

	private static void rememberOverworldReturnPosition(ServerPlayer player) {
		if (player.level().dimension() != Level.OVERWORLD) {
			return;
		}

		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		@Nullable BlockPos portalPos = findNearbyPortalBlock(player.level(), playerPos, OVERWORLD_PORTAL_SCAN_RADIUS);
		BlockPos returnPos = portalPos == null ? playerPos : safePortalReturnPos(player.level(), portalPos);
		OVERWORLD_RETURN_POSITIONS.put(player.getUUID(), returnPos);
	}

	private static @Nullable BlockPos findNearbyPortalBlock(ServerLevel level, BlockPos center, int radius) {
		@Nullable BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (!level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
						continue;
					}
					double distance = center.distSqr(pos);
					if (distance < nearestDistance) {
						nearest = pos;
						nearestDistance = distance;
					}
				}
			}
		}
		return nearest;
	}

	private static BlockPos safePortalReturnPos(ServerLevel level, BlockPos portalPos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos candidate = portalPos.relative(direction);
			if (canStandAt(level, candidate)) {
				return candidate;
			}
			BlockPos below = candidate.below();
			if (canStandAt(level, below)) {
				return below;
			}
		}
		return portalPos;
	}

	private static boolean canStandAt(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos.below()).isAir()
				&& level.getBlockState(pos).isAir()
				&& level.getBlockState(pos.above()).isAir();
	}

	private static void prepareFortressWarpedPortal(ServerPlayer player, EndrRTAConfig config) {
		if (!config.allowsGenerateFortressWarpedPortal() || player.level().dimension() != Level.NETHER) {
			return;
		}
		if (FORTRESS_WARPED_PORTALS.containsKey(player.getUUID())) {
			return;
		}
		if (FORTRESS_WARPED_PORTAL_FAILED.contains(player.getUUID())) {
			return;
		}

		ServerLevel level = player.level();
		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		@Nullable FortressWarpedPair pair = findFortressWarpedPair(level, playerPos);
		if (pair == null) {
			markFortressWarpedPortalFailed(player, "[EnderRTA] ネザー要塞と青森の組み合わせが見つからなかったため、要塞青森ゲートを生成できませんでした。");
			return;
		}
		BlockPos fortress = pair.fortress();
		BlockPos warpedForest = pair.warpedForest();
		double fortressWarpedDistance = pair.distance();

		@Nullable BlockPos existingPortal = findExistingFortressWarpedPortal(level, warpedForest, fortress);
		if (existingPortal != null) {
			registerFortressWarpedPortal(player, existingPortal);
			movePlayerToFortressWarpedPortal(player, level, existingPortal);
			player.sendSystemMessage(Component.literal("[EnderRTA] 既存の要塞青森ゲートを使用します: "
					+ existingPortal.getX() + " " + existingPortal.getY() + " " + existingPortal.getZ()
					+ " / 要塞-青森距離: " + (int) Math.round(fortressWarpedDistance) + "m"));
			return;
		}

		@Nullable BlockPos portalBase = findSafePortalBase(level, warpedForest, fortress);
		if (portalBase == null) {
			markFortressWarpedPortalFailed(player, "[EnderRTA] ネザー要塞近くの青森内にゲートを置ける場所が見つかりませんでした。");
			return;
		}

		buildNetherPortal(level, portalBase);
		registerFortressWarpedPortal(player, portalBase);
		movePlayerToFortressWarpedPortal(player, level, portalBase);
		player.sendSystemMessage(Component.literal("[EnderRTA] 要塞青森ゲートを生成しました: "
				+ portalBase.getX() + " " + portalBase.getY() + " " + portalBase.getZ()
				+ " / 要塞-青森距離: " + (int) Math.round(fortressWarpedDistance) + "m"));
	}

	private static void registerFortressWarpedPortal(ServerPlayer player, BlockPos portalPos) {
		FORTRESS_WARPED_PORTALS.put(player.getUUID(), portalPos);
		FORTRESS_WARPED_PORTAL_FAILED.remove(player.getUUID());
	}

	private static void movePlayerToFortressWarpedPortal(ServerPlayer player, ServerLevel level, BlockPos portalPos) {
		BlockPos safePos = safePortalReturnPos(level, portalPos);
		player.setPortalCooldown(100);
		player.teleportTo(
				level,
				safePos.getX() + 0.5D,
				safePos.getY(),
				safePos.getZ() + 0.5D,
				ABSOLUTE_TELEPORT,
				player.getYRot(),
				player.getXRot(),
				false
		);
	}

	private static void markFortressWarpedPortalFailed(ServerPlayer player, String message) {
		if (FORTRESS_WARPED_PORTAL_FAILED.add(player.getUUID())) {
			player.sendSystemMessage(Component.literal(message));
		}
	}

	private static void handleFortressWarpedPortalReturn(ServerPlayer player, EndrRTAConfig config) {
		if (!config.allowsGenerateFortressWarpedPortal() || player.level().dimension() != Level.NETHER) {
			return;
		}
		@Nullable BlockPos portalBase = FORTRESS_WARPED_PORTALS.get(player.getUUID());
		if (portalBase == null || !isInsideGeneratedPortal(player, portalBase)) {
			return;
		}
		@Nullable BlockPos returnPos = OVERWORLD_RETURN_POSITIONS.get(player.getUUID());
		if (returnPos == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] 元ゲートの位置が記録されていないため、通常のポータル移動に任せます。"));
			return;
		}
		@Nullable ServerLevel overworld = player.level().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			return;
		}

		player.setPortalCooldown(100);
		player.teleportTo(
				overworld,
				returnPos.getX() + 0.5D,
				returnPos.getY(),
				returnPos.getZ() + 0.5D,
				ABSOLUTE_TELEPORT,
				player.getYRot(),
				player.getXRot(),
				false
		);
		player.sendSystemMessage(Component.literal("[EnderRTA] 元のゲートへ戻りました。"));
	}

	private static boolean isInsideGeneratedPortal(ServerPlayer player, BlockPos portalBase) {
		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		if (Math.sqrt(playerPos.distSqr(portalBase)) > GENERATED_PORTAL_TRIGGER_RADIUS) {
			return false;
		}
		return player.level().getBlockState(playerPos).is(Blocks.NETHER_PORTAL)
				|| player.level().getBlockState(playerPos.above()).is(Blocks.NETHER_PORTAL);
	}

	private static @Nullable BlockPos findNearestWarpedForest(ServerLevel level, BlockPos origin) {
		return findNearestWarpedForest(level, origin, WARPED_FOREST_SEARCH_RADIUS);
	}

	private static @Nullable BlockPos findNearestWarpedForest(ServerLevel level, BlockPos origin, int radius) {
		@Nullable Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
				EndrRTAServerState::isWarpedForest,
				origin,
				radius,
				32,
				64
		);
		return found == null ? null : found.getFirst();
	}

	private static @Nullable FortressWarpedPair findFortressWarpedPair(ServerLevel level, BlockPos playerPos) {
		@Nullable FortressWarpedPair best = null;
		for (int radius = 0; radius <= FORTRESS_WARPED_PAIR_SEARCH_RADIUS; radius += FORTRESS_WARPED_PAIR_SEARCH_STEP) {
			for (int dx = -radius; dx <= radius; dx += FORTRESS_WARPED_PAIR_SEARCH_STEP) {
				for (int dz = -radius; dz <= radius; dz += FORTRESS_WARPED_PAIR_SEARCH_STEP) {
					if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
						continue;
					}

					BlockPos origin = playerPos.offset(dx, 0, dz);
					best = closerPair(playerPos, best, findFortressWarpedPairFromFortress(level, origin));
					best = closerPair(playerPos, best, findFortressWarpedPairFromWarpedForest(level, origin));
				}
			}
		}
		return best;
	}

	private static @Nullable FortressWarpedPair findFortressWarpedPairFromFortress(ServerLevel level, BlockPos origin) {
		@Nullable BlockPos fortress = level.findNearestMapStructure(
				EndrRTATags.NETHER_FORTRESS,
				origin,
				FORTRESS_PORTAL_STRUCTURE_SEARCH_RADIUS,
				false
		);
		if (fortress == null) {
			return null;
		}

		@Nullable BlockPos warpedForest = findNearestWarpedForest(level, fortress);
		return createFortressWarpedPair(fortress, warpedForest);
	}

	private static @Nullable FortressWarpedPair findFortressWarpedPairFromWarpedForest(ServerLevel level, BlockPos origin) {
		@Nullable BlockPos warpedForest = findNearestWarpedForest(level, origin, FORTRESS_PORTAL_STRUCTURE_SEARCH_RADIUS);
		if (warpedForest == null) {
			return null;
		}

		@Nullable BlockPos fortress = level.findNearestMapStructure(
				EndrRTATags.NETHER_FORTRESS,
				warpedForest,
				FORTRESS_PORTAL_STRUCTURE_SEARCH_RADIUS,
				false
		);
		return createFortressWarpedPair(fortress, warpedForest);
	}

	private static @Nullable FortressWarpedPair createFortressWarpedPair(@Nullable BlockPos fortress, @Nullable BlockPos warpedForest) {
		if (fortress == null || warpedForest == null) {
			return null;
		}

		double distance = horizontalDistance(fortress, warpedForest);
		if (distance > WARPED_FOREST_MAX_DISTANCE) {
			return null;
		}
		return new FortressWarpedPair(fortress, warpedForest, distance);
	}

	private static @Nullable FortressWarpedPair closerPair(BlockPos playerPos, @Nullable FortressWarpedPair current, @Nullable FortressWarpedPair candidate) {
		if (candidate == null) {
			return current;
		}
		if (current == null) {
			return candidate;
		}
		return horizontalDistanceSqr(playerPos, candidate.warpedForest()) < horizontalDistanceSqr(playerPos, current.warpedForest())
				? candidate
				: current;
	}

	private static boolean isWarpedForest(Holder<Biome> biome) {
		return biome.unwrapKey()
				.map(ResourceKey::identifier)
				.map(WARPED_FOREST_ID::equals)
				.orElse(false);
	}

	private static boolean isWarpedForest(ServerLevel level, BlockPos pos) {
		return isWarpedForest(level.getBiome(pos));
	}

	private static void assistEndermenIntoBoats(ServerPlayer player, EndrRTAConfig config) {
		if (!config.allowsGuideEndermenToBoats() || player.level().dimension() != Level.NETHER || radarTick % 5 != 0) {
			return;
		}

		ServerLevel level = player.level();
		if (!isWarpedForest(level, player.blockPosition())) {
			return;
		}

		double assistRadiusSqr = ENDERMAN_BOAT_ASSIST_RADIUS * ENDERMAN_BOAT_ASSIST_RADIUS;
		List<? extends AbstractBoat> boats = level.getEntities(EntityTypeTest.forClass(AbstractBoat.class), boat ->
				boat.isAlive()
						&& boat.getPassengers().size() < 2
						&& boat.distanceToSqr(player) <= assistRadiusSqr
						&& isWarpedForest(level, boat.blockPosition()));
		Set<EnderMan> assignedEndermen = new HashSet<>();
		for (AbstractBoat boat : boats) {
			@Nullable EnderMan enderman = findNearestAvailableEnderman(level, boat, assignedEndermen);
			if (enderman == null) {
				continue;
			}

			assignedEndermen.add(enderman);
			if (enderman.distanceToSqr(boat) <= ENDERMAN_BOAT_BOARD_DISTANCE * ENDERMAN_BOAT_BOARD_DISTANCE) {
				enderman.startRiding(boat, true, true);
			} else {
				guideEndermanToBoat(level, enderman, boat);
			}
		}
	}

	private static void guideEndermanToBoat(ServerLevel level, EnderMan enderman, AbstractBoat boat) {
		boolean canNavigate = enderman.getNavigation().moveTo(boat.getX(), boat.getY(), boat.getZ(), ENDERMAN_BOAT_NAVIGATION_SPEED);
		if (canNavigate) {
			return;
		}

		playEndermanBoatWarpEffect(level, enderman.getX(), enderman.getY(), enderman.getZ());
		enderman.teleportTo(boat.getX(), boat.getY(), boat.getZ());
		playEndermanBoatWarpEffect(level, boat.getX(), boat.getY(), boat.getZ());
		enderman.startRiding(boat, true, true);
	}

	private static void playEndermanBoatWarpEffect(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z, 48, 0.45D, 0.9D, 0.45D, 0.08D);
		level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	private static @Nullable EnderMan findNearestAvailableEnderman(ServerLevel level, AbstractBoat boat, Set<EnderMan> assignedEndermen) {
		double assistRadiusSqr = ENDERMAN_BOAT_ASSIST_RADIUS * ENDERMAN_BOAT_ASSIST_RADIUS;
		List<? extends EnderMan> endermen = level.getEntities(EntityType.ENDERMAN, enderman ->
				enderman.isAlive()
						&& !enderman.isPassenger()
						&& !assignedEndermen.contains(enderman)
						&& enderman.distanceToSqr(boat) <= assistRadiusSqr
						&& isWarpedForest(level, enderman.blockPosition()));

		@Nullable EnderMan nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (EnderMan enderman : endermen) {
			double distance = enderman.distanceToSqr(boat);
			if (distance < nearestDistance) {
				nearest = enderman;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private static @Nullable BlockPos findSafePortalBase(ServerLevel level, BlockPos center, BlockPos fortress) {
		for (int radius = 0; radius <= PORTAL_SAFE_POS_SEARCH_RADIUS; radius += 4) {
			for (int dx = -radius; dx <= radius; dx += 4) {
				for (int dz = -radius; dz <= radius; dz += 4) {
					if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
						continue;
					}
					for (int y = 32; y <= 117; y++) {
						BlockPos base = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
						if (isWarpedForest(level, base)
								&& horizontalDistance(fortress, base) <= WARPED_FOREST_MAX_DISTANCE
								&& canPlacePortal(level, base)) {
							return base;
						}
					}
				}
			}
		}
		return null;
	}

	private static @Nullable BlockPos findExistingFortressWarpedPortal(ServerLevel level, BlockPos center, BlockPos fortress) {
		@Nullable BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (int dx = -PORTAL_SAFE_POS_SEARCH_RADIUS; dx <= PORTAL_SAFE_POS_SEARCH_RADIUS; dx++) {
			for (int dz = -PORTAL_SAFE_POS_SEARCH_RADIUS; dz <= PORTAL_SAFE_POS_SEARCH_RADIUS; dz++) {
				for (int y = 32; y <= 117; y++) {
					BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
					if (!level.getBlockState(pos).is(Blocks.NETHER_PORTAL)
							|| !isWarpedForest(level, pos)
							|| horizontalDistance(fortress, pos) > WARPED_FOREST_MAX_DISTANCE) {
						continue;
					}

					double distance = horizontalDistanceSqr(center, pos);
					if (distance < nearestDistance) {
						nearest = pos;
						nearestDistance = distance;
					}
				}
			}
		}
		return nearest;
	}

	private static boolean canPlacePortal(ServerLevel level, BlockPos base) {
		for (int x = -1; x <= 4; x++) {
			for (int y = -1; y <= 5; y++) {
				for (int z = -1; z <= 1; z++) {
					BlockState state = level.getBlockState(base.offset(x, y, z));
					if (state.is(Blocks.BEDROCK)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static double horizontalDistance(BlockPos first, BlockPos second) {
		return Math.sqrt(horizontalDistanceSqr(first, second));
	}

	private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
		double dx = first.getX() - second.getX();
		double dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private record FortressWarpedPair(BlockPos fortress, BlockPos warpedForest, double distance) {
	}

	private static void buildNetherPortal(ServerLevel level, BlockPos base) {
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		BlockState portal = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Direction.Axis.X);
		BlockState air = Blocks.AIR.defaultBlockState();

		for (int x = -1; x <= 4; x++) {
			for (int y = 0; y <= 5; y++) {
				for (int z = -1; z <= 1; z++) {
					level.setBlock(base.offset(x, y, z), air, 3);
				}
			}
		}
		for (int x = -1; x <= 4; x++) {
			for (int z = -1; z <= 1; z++) {
				level.setBlock(base.offset(x, -1, z), obsidian, 3);
			}
		}
		for (int x = 0; x <= 3; x++) {
			for (int y = 0; y <= 4; y++) {
				boolean frame = x == 0 || x == 3 || y == 0 || y == 4;
				level.setBlock(base.offset(x, y, 0), frame ? obsidian : portal, 3);
			}
		}
	}

	private static void handleTimer(ServerPlayer player, RunState run, EndrRTAConfig config) {
		run.startIfNeeded(player.level().dimension());
		run.tickIgt();

		if (!config.autoSplits) {
			run.setLastDimension(player.level().dimension());
			return;
		}

		if (run.lastDimension() != player.level().dimension()) {
			if (player.level().dimension() == Level.NETHER) {
				run.recordSplit(SplitType.NETHER_ENTER);
			} else if (player.level().dimension() == Level.END) {
				run.recordSplit(SplitType.END_ENTER);
			}
			run.setLastDimension(player.level().dimension());
		}
	}

	private static void updateRadar(ServerPlayer player, RunState run, EndrRTAConfig config) {
		if (!config.allowsRadar()) {
			run.setStronghold(null);
			run.setFortress(null);
			run.setWarpedForestFromFortress(null);
			run.setBastionType(BASTION_UNKNOWN);
			return;
		}

		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		if (player.level().dimension() == Level.OVERWORLD) {
			run.setFortress(null);
			run.setWarpedForestFromFortress(null);
			run.setBastionType(BASTION_UNKNOWN);
			RadarTarget stronghold = findTarget(player.level(), "エンド要塞", StructureTags.EYE_OF_ENDER_LOCATED, playerPos, config.radarSearchRadius);
			run.setStronghold(stronghold);
			if (stronghold != null && stronghold.distance() <= config.structureFoundDistance) {
				run.recordSplit(SplitType.STRONGHOLD_FOUND);
			}
		} else if (player.level().dimension() == Level.NETHER) {
			run.setStronghold(null);
			RadarTarget fortress = findTarget(player.level(), "ネザー要塞", EndrRTATags.NETHER_FORTRESS, playerPos, config.radarSearchRadius);
			run.setFortress(fortress);
			run.setWarpedForestFromFortress(findWarpedForestTargetFromFortress(player.level(), fortress));
			if (!config.showBastionType) {
				run.setBastionType(BASTION_UNKNOWN);
				run.setBastionFound(false);
			} else {
				// Try to find bastion target within search radius
				RadarTarget bastionTarget = findTarget(player.level(), "ピグリン要塞", EndrRTATags.BASTION_REMNANT, playerPos, config.radarSearchRadius);
				Optional<String> enteredBastionType = findEnteredBastionType(player.level(), playerPos);
				if (enteredBastionType.isPresent()) {
					run.setBastionType(enteredBastionType.get());
					run.setBastionFound(true);
				} else if (bastionTarget == null) {
					// No bastion in search radius
					if (run.bastionFound()) {
						// Previously found but now out of search radius -> stop showing it
						run.setBastionFound(false);
						run.setBastionType(BASTION_UNKNOWN);
					} else {
						run.setBastionType(BASTION_UNKNOWN);
					}
				} else {
					// Only treat as found when within the closer 'structureFoundDistance' threshold
					if (bastionTarget.distance() <= config.structureFoundDistance) {
						String type = identifyBastionType(player.level(), bastionTarget.pos()).orElse("不明");
						run.setBastionType(type);
						run.setBastionFound(true);
					} else {
						// Bastion detected in radar radius but not close enough
						if (run.bastionFound()) {
							// Previously found earlier; if we moved away, stop showing it
							run.setBastionFound(false);
							run.setBastionType(BASTION_UNKNOWN);
						} else {
							run.setBastionType(BASTION_UNKNOWN);
						}
					}
				}
			}
			if (fortress != null && fortress.distance() <= config.structureFoundDistance) {
				run.recordSplit(SplitType.NETHER_FORTRESS_FOUND);
			}
		} else {
			run.setStronghold(null);
			run.setFortress(null);
			run.setWarpedForestFromFortress(null);
			run.setBastionType(BASTION_UNKNOWN);
		}
	}

	private static @Nullable RadarTarget findWarpedForestTargetFromFortress(ServerLevel level, @Nullable RadarTarget fortress) {
		if (fortress == null) {
			return null;
		}
		@Nullable BlockPos warpedForest = findNearestWarpedForest(level, fortress.pos());
		if (warpedForest == null) {
			return null;
		}
		return new RadarTarget("青森", warpedForest, Math.sqrt(fortress.pos().distSqr(warpedForest)));
	}

	private static @Nullable RadarTarget findTarget(ServerLevel level, String label, @NonNull TagKey<Structure> tag, @NonNull BlockPos origin, int radius) {
		@Nullable BlockPos found = level.findNearestMapStructure(tag, origin, radius, false);
		if (found == null) {
			return null;
		}

		double distance = Math.sqrt(origin.distSqr(found));
		return new RadarTarget(label, found, distance);
	}

	private static Optional<String> findEnteredBastionType(ServerLevel level, BlockPos playerPos) {
		Set<Structure> bastionStructures = new HashSet<>();
		for (Holder<Structure> holder : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getTagOrEmpty(EndrRTATags.BASTION_REMNANT)) {
			bastionStructures.add(holder.value());
		}
		if (bastionStructures.isEmpty()) {
			return Optional.empty();
		}

		ChunkAccess chunk = level.getChunk(playerPos.getX() >> 4, playerPos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS, true);
		for (Structure structure : bastionStructures) {
			StructureStart start = chunk.getStartForStructure(structure);
			if (start != null && start.isValid() && start.getBoundingBox().isInside(playerPos)) {
				return bastionTypeFromStart(start).or(() -> Optional.of("不明"));
			}
		}
		return Optional.empty();
	}

	private static String findBastionType(ServerLevel level, @NonNull BlockPos origin, int radius) {
		RadarTarget bastion = findTarget(level, "ピグリン要塞", EndrRTATags.BASTION_REMNANT, origin, radius);
		if (bastion == null) {
			return BASTION_UNKNOWN;
		}
		return identifyBastionType(level, bastion.pos()).orElse("不明");
	}

	private static Optional<String> identifyBastionType(ServerLevel level, BlockPos bastionPos) {
		Set<Structure> bastionStructures = new HashSet<>();
		for (Holder<Structure> holder : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getTagOrEmpty(EndrRTATags.BASTION_REMNANT)) {
			bastionStructures.add(holder.value());
		}
		if (bastionStructures.isEmpty()) {
			return Optional.empty();
		}

		ChunkPos center = new ChunkPos(bastionPos.getX() >> 4, bastionPos.getZ() >> 4);
		for (int radius = 0; radius <= BASTION_START_SCAN_RADIUS_CHUNKS; radius++) {
			Optional<String> found = scanBastionTypeRing(level, center, radius, bastionStructures);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}

	private static Optional<String> scanBastionTypeRing(ServerLevel level, ChunkPos center, int radius, Set<Structure> bastionStructures) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
					continue;
				}
				ChunkAccess chunk = level.getChunk(center.x() + dx, center.z() + dz, ChunkStatus.STRUCTURE_STARTS, true);
				for (Structure structure : bastionStructures) {
					StructureStart start = chunk.getStartForStructure(structure);
					if (start != null && start.isValid()) {
						Optional<String> type = bastionTypeFromStart(start);
						if (type.isPresent()) {
							return type;
						}
					}
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<String> bastionTypeFromStart(StructureStart start) {
		boolean units = false;
		boolean bridge = false;
		boolean hoglinStable = false;
		boolean treasure = false;
		for (StructurePiece piece : start.getPieces()) {
			String descriptor = bastionPieceDescriptor(piece);
			if (descriptor.contains("bastion/treasure/")) {
				treasure = true;
			} else if (descriptor.contains("bastion/hoglin_stable/")) {
				hoglinStable = true;
			} else if (descriptor.contains("bastion/bridge/")) {
				bridge = true;
			} else if (descriptor.contains("bastion/units/")) {
				units = true;
			}
		}
		if (treasure) {
			return Optional.of("宝物部屋");
		}
		if (hoglinStable) {
			return Optional.of("ホグリン小屋");
		}
		if (bridge) {
			return Optional.of("橋");
		}
		if (units) {
			return Optional.of("住居");
		}
		return Optional.empty();
	}

	private static String bastionPieceDescriptor(StructurePiece piece) {
		if (piece instanceof PoolElementStructurePiece poolPiece) {
			StructurePoolElement element = poolPiece.getElement();
			if (element instanceof SinglePoolElement singleElement) {
				return singleElement.getTemplateLocation().toString();
			}
			return element.toString();
		}
		return piece.toString();
	}
}
