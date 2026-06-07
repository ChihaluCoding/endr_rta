package chihalu.endrrta.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.entity.Relative;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.EndrRTA;
import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class EndrRTAServerState {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final @NonNull Set<@NonNull Relative> ABSOLUTE_TELEPORT = Set.of();
	private static final Map<UUID, RunState> RUNS = new HashMap<>();
	private static final Set<UUID> VILLAGE_SPAWNED = new HashSet<>();
	private static final String STATE_FILE_NAME = "endrrta-state.json";
	private static final int MIN_SAFE_SURFACE_OFFSET = 5;
	private static final int BASTION_START_SCAN_RADIUS_CHUNKS = 12;
	private static final String BASTION_UNKNOWN = "未検出";
	private static int radarTick;

	private EndrRTAServerState() {
	}

	public static void tickServer(MinecraftServer server) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		radarTick++;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RunState run = RUNS.computeIfAbsent(player.getUUID(), ignored -> new RunState());
			prepareVillageSpawn(player, config);
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
		VILLAGE_SPAWNED.clear();
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
			VILLAGE_SPAWNED.clear();
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
			run.setBastionType(BASTION_UNKNOWN);
			return;
		}

		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		if (player.level().dimension() == Level.OVERWORLD) {
			run.setFortress(null);
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
			run.setBastionType(BASTION_UNKNOWN);
		}
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
