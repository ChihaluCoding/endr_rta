package chihalu.endrrta.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class HayBaleClusterBreaker {
	private static final int HORIZONTAL_RADIUS = 2;
	private static final int VERTICAL_RADIUS = 1;
	private static final ThreadLocal<Boolean> BREAKING_CLUSTER = ThreadLocal.withInitial(() -> false);

	private HayBaleClusterBreaker() {
	}

	public static void breakNearbyHayBales(Level level, Player player, BlockPos origin, BlockState brokenState, BlockEntity blockEntity) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		if (!config.allowsPracticeAssist() || !config.breakNearbyHayBales || BREAKING_CLUSTER.get()) {
			return;
		}
		if (!(level instanceof ServerLevel serverLevel) || !brokenState.is(Blocks.HAY_BLOCK)) {
			return;
		}

		BREAKING_CLUSTER.set(true);
		try {
			for (BlockPos pos : BlockPos.betweenClosed(
					origin.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS, -HORIZONTAL_RADIUS),
					origin.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS)
			)) {
				if (pos.equals(origin) || !serverLevel.getBlockState(pos).is(Blocks.HAY_BLOCK)) {
					continue;
				}
				serverLevel.destroyBlock(pos.immutable(), true, player, 512);
			}
		} finally {
			BREAKING_CLUSTER.set(false);
		}
	}
}
