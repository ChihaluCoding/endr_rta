package chihalu.endrrta.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfigManager;

public final class PracticeHubButtonHandler {
	private static final Set<Relative> ABSOLUTE_TELEPORT = Set.of();
	private static final int DESTINATION_SCAN_RADIUS = 12;

	private PracticeHubButtonHandler() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			BlockPos clickedPos = hitResult.getBlockPos();
			if (level.isClientSide()) {
				if (level.getBlockEntity(clickedPos) instanceof SignBlockEntity sign
						&& (PracticeSignSyncState.hasPendingSign() || isPracticeControlledSign(sign))) {
					return InteractionResult.SUCCESS;
				}
				return InteractionResult.PASS;
			}
			if (!(level instanceof ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}
			if (level.getBlockEntity(clickedPos) instanceof SignBlockEntity sign) {
				InteractionResult signApplyResult = PracticeSignCommand.applyPendingSign(serverPlayer, serverLevel, clickedPos, sign);
				if (signApplyResult != InteractionResult.PASS) {
					return signApplyResult;
				}
				if (isPracticeMenuSign(sign)) {
					return openPracticeSelectionMenu(serverPlayer, serverLevel, clickedPos);
				}
				if (isPracticeDestinationSign(sign)) {
					return InteractionResult.SUCCESS;
				}
			}
			if (!level.getBlockState(clickedPos).is(Blocks.STONE_BUTTON)) {
				return InteractionResult.PASS;
			}
			return handlePracticeButton(serverPlayer, serverLevel, clickedPos);
		});
	}

	private static InteractionResult openPracticeSelectionMenu(ServerPlayer player, ServerLevel level, BlockPos menuSignPos) {
		Map<String, PracticeDestination> destinations = new HashMap<>(findNearbyDestinations(level, menuSignPos));
		destinations.putAll(PracticeStartPointManager.destinations());
		PracticeSelectionMenu.openMain(player, destinations);
		if (destinations.isEmpty()) {
			player.sendSystemMessage(Component.literal("[EnderRTA] TP先が未設定です。メニュー項目をクリックしてから /setstartpoint x y z ow/nether/end で設定してください。"));
		}
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult handlePracticeButton(ServerPlayer player, ServerLevel level, BlockPos buttonPos) {
		@Nullable PracticeDestination destination = findDestination(level, buttonPos);
		if (destination == null) {
			return InteractionResult.PASS;
		}

		@Nullable ServerLevel targetLevel = level.getServer().getLevel(destination.dimension());
		if (targetLevel == null) {
			player.sendSystemMessage(Component.literal("[EnderRTA] 練習場のディメンションが見つかりません: " + destination.dimension().identifier()));
			return InteractionResult.SUCCESS;
		}

		EndrRTAConfigManager.get().practiceScenario = destination.scenarioId();
		EndrRTAConfigManager.save();
		BlockPos target = destination.pos();
		player.teleportTo(
				targetLevel,
				target.getX() + 0.5D,
				target.getY(),
				target.getZ() + 0.5D,
				ABSOLUTE_TELEPORT,
				player.getYRot(),
				player.getXRot(),
				false
		);
		player.sendSystemMessage(Component.literal("[EnderRTA] " + destination.label() + " 練習場へ移動しました。"));
		return InteractionResult.SUCCESS;
	}

	private static @Nullable PracticeDestination findDestination(ServerLevel level, BlockPos buttonPos) {
		for (Direction direction : Direction.values()) {
			BlockPos signPos = buttonPos.relative(direction);
			if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
				@Nullable PracticeDestination destination = readDestination(sign);
				if (destination != null) {
					return destination;
				}
			}
		}
		return null;
	}

	private static Map<String, PracticeDestination> findNearbyDestinations(ServerLevel level, BlockPos center) {
		Map<String, PracticeDestination> destinations = new HashMap<>();
		BlockPos from = center.offset(-DESTINATION_SCAN_RADIUS, -DESTINATION_SCAN_RADIUS, -DESTINATION_SCAN_RADIUS);
		BlockPos to = center.offset(DESTINATION_SCAN_RADIUS, DESTINATION_SCAN_RADIUS, DESTINATION_SCAN_RADIUS);
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
				@Nullable PracticeDestination destination = readDestination(sign);
				if (destination != null) {
					destinations.putIfAbsent(destination.scenarioId(), destination);
				}
			}
		}
		return Map.copyOf(destinations);
	}

	private static boolean isPracticeMenuSign(SignBlockEntity sign) {
		return isPracticeMenuText(sign.getFrontText()) || isPracticeMenuText(sign.getBackText());
	}

	private static boolean isPracticeDestinationSign(SignBlockEntity sign) {
		return readDestination(sign) != null;
	}

	private static boolean isPracticeControlledSign(SignBlockEntity sign) {
		return isPracticeMenuSign(sign) || isPracticeDestinationSign(sign);
	}

	private static boolean isPracticeMenuText(SignText text) {
		for (int index = 0; index < 4; index++) {
			String value = line(text, index);
			if ("ゲーム選択".equals(value) || "練習選択".equals(value) || "個別練習".equals(value)) {
				return true;
			}
		}
		return false;
	}

	private static @Nullable PracticeDestination readDestination(SignBlockEntity sign) {
		if ("[EndrRTA]".equalsIgnoreCase(line(sign.getFrontText(), 0))) {
			return readDestination(sign.getFrontText());
		}
		if ("[EndrRTA]".equalsIgnoreCase(line(sign.getBackText(), 0))) {
			return readDestination(sign.getBackText());
		}
		return null;
	}

	private static @Nullable PracticeDestination readDestination(SignText text) {
		String scenarioId = scenarioId(line(text, 1));
		if ("none".equals(scenarioId)) {
			return null;
		}

		try {
			ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(line(text, 2)));
			String[] parts = line(text, 3).split("[,\\s]+");
			if (parts.length < 3) {
				return null;
			}
			BlockPos pos = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			return new PracticeDestination(scenarioId, scenarioLabel(scenarioId), dimension, pos);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static String line(SignText text, int index) {
		return text.getMessage(index, false).getString().trim();
	}

	private static String scenarioId(String value) {
		return switch (value.trim()) {
			case "nether_fortress", "ネザー要塞" -> "nether_fortress";
			case "bastion", "ピグリン要塞" -> "bastion";
			case "warped_forest_pearls", "歪んだ森エンパ", "エンパ" -> "warped_forest_pearls";
			case "lava_pool_portal", "溶岩ゲート", "マグマ溜まりゲート" -> "lava_pool_portal";
			case "ender_dragon", "エンドラ討伐", "エンドラ" -> "ender_dragon";
			default -> "none";
		};
	}

	private static String scenarioLabel(String scenarioId) {
		return PracticeStartPointManager.label(scenarioId);
	}

}
