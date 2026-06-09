package chihalu.endrrta.server;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

public final class PracticeSignCommand {
	private static final Map<UUID, PendingSign> PENDING_SIGNS = new ConcurrentHashMap<>();

	private PracticeSignCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("setsign")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(argument("name", string())
								.executes(context -> prepareSign(context.getSource(), getString(context, "name"))))
		));
	}

	public static InteractionResult applyPendingSign(ServerPlayer player, ServerLevel level, BlockPos signPos, SignBlockEntity sign) {
		PendingSign pending = PENDING_SIGNS.remove(player.getUUID());
		if (pending == null) {
			return InteractionResult.PASS;
		}
		sendPendingState(player, false);

		boolean visibleSide = sign.isFacingFrontText(player);
		sign.setText(visibleText(sign.getText(visibleSide), pending), visibleSide);
		sign.setText(menuMarkerText(sign.getText(!visibleSide)), !visibleSide);
		sign.setChanged();
		BlockState state = level.getBlockState(signPos);
		level.sendBlockUpdated(signPos, state, state, 3);
		player.sendSystemMessage(Component.literal("[EnderRTA] メニュー看板を設定しました: " + pending.name()));
		return InteractionResult.SUCCESS;
	}

	private static int prepareSign(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		PENDING_SIGNS.put(player.getUUID(), new PendingSign(name));
		sendPendingState(player, true);
		source.sendSuccess(() -> Component.literal(
				"[EnderRTA] 次に右クリックした看板をメニュー看板にします: " + name
		), false);
		return 1;
	}

	private static void sendPendingState(ServerPlayer player, boolean pending) {
		PracticeSignSyncState.setPendingSign(pending);
		if (ServerPlayNetworking.canSend(player, PracticeSignSyncPayload.TYPE)) {
			ServerPlayNetworking.send(player, new PracticeSignSyncPayload(pending));
		}
	}

	private static SignText visibleText(SignText text, PendingSign pending) {
		return text
				.setMessage(0, Component.literal(pending.name()))
				.setMessage(1, Component.empty())
				.setMessage(2, Component.empty())
				.setMessage(3, Component.empty());
	}

	private static SignText menuMarkerText(SignText text) {
		return text
				.setMessage(0, Component.literal("ゲーム選択"))
				.setMessage(1, Component.empty())
				.setMessage(2, Component.empty())
				.setMessage(3, Component.empty());
	}

	private record PendingSign(String name) {
	}
}
