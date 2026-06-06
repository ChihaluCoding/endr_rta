package chihalu.endrrta.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.entity.Relative;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class EndrRTAServerState {
	private static final @NonNull Set<@NonNull Relative> ABSOLUTE_TELEPORT = Set.of();
	private static final Map<UUID, RunState> RUNS = new HashMap<>();
	private static final Set<UUID> VILLAGE_SPAWNED = new HashSet<>();
	private static int radarTick;

	private EndrRTAServerState() {
	}

	public static void tickServer(MinecraftServer server) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		radarTick++;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			RunState run = RUNS.computeIfAbsent(player.getUUID(), ignored -> new RunState());
			handleVillageSpawn(player, config);
			handleTimer(player, run, config);
			if (radarTick % 100 == 0) {
				updateRadar(player, run, config);
			}
		}
	}

	public static @Nullable RunState getRun(UUID playerId) {
		return RUNS.get(playerId);
	}

	public static void clear() {
		RUNS.clear();
		VILLAGE_SPAWNED.clear();
		radarTick = 0;
	}

	public static void stopForDragon(ServerLevel level) {
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			RunState run = RUNS.get(player.getUUID());
			if (run != null) {
				run.stopAtDragon();
			}
		}
	}

	private static void handleVillageSpawn(ServerPlayer player, EndrRTAConfig config) {
		if (!config.allowsPracticeAssist() || !config.forceVillageSpawn || player.level().dimension() != Level.OVERWORLD) {
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
			player.sendSystemMessage(Component.literal("[EndrRTA] 指定範囲内に村が見つかりませんでした。"));
			return;
		}

		BlockPos safePos = player.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, village).above();
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
		player.sendSystemMessage(Component.literal("[EndrRTA] 最寄り村へスポーン補正しました。"));
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
		if (!config.allowsPracticeAssist() || !config.showRadar) {
			run.setStronghold(null);
			run.setFortress(null);
			return;
		}

		BlockPos playerPos = Objects.requireNonNull(player.blockPosition(), "player block position");
		if (player.level().dimension() == Level.OVERWORLD) {
			RadarTarget stronghold = findTarget(player.level(), "エンド要塞", StructureTags.EYE_OF_ENDER_LOCATED, playerPos, config.radarSearchRadius);
			run.setStronghold(stronghold);
			if (stronghold != null && stronghold.distance() <= config.structureFoundDistance) {
				run.recordSplit(SplitType.STRONGHOLD_FOUND);
			}
		}

		if (player.level().dimension() == Level.NETHER) {
			RadarTarget fortress = findTarget(player.level(), "ネザー要塞", EndrRTATags.NETHER_FORTRESS, playerPos, config.radarSearchRadius);
			run.setFortress(fortress);
			if (fortress != null && fortress.distance() <= config.structureFoundDistance) {
				run.recordSplit(SplitType.NETHER_FORTRESS_FOUND);
			}
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
}
