package chihalu.endrrta.server;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Locale;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class PracticeStartPointCommand {
	private static final SimpleCommandExceptionType INVALID_DIMENSION = new SimpleCommandExceptionType(
			Component.literal("ワールド種類は ow / nether / end のどれかを指定してください。")
	);
	private static final SimpleCommandExceptionType NO_SELECTED_SCENARIO = new SimpleCommandExceptionType(
			Component.literal("先に練習メニューでTP先を設定したい項目をクリックしてください。")
	);
	private static final SimpleCommandExceptionType INVALID_SCENARIO = new SimpleCommandExceptionType(
			Component.literal("項目名が不正です。例: housing / bridge / treasure / random / hoglin")
	);

	private PracticeStartPointCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("setstartpoint")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(argument("scenario", word())
								.then(argument("pos", BlockPosArgument.blockPos())
										.then(argument("dimension", word())
												.executes(context -> setStartPoint(
														context.getSource(),
														getString(context, "scenario"),
														BlockPosArgument.getBlockPos(context, "pos"),
														getString(context, "dimension")
												)))))
						.then(argument("pos", BlockPosArgument.blockPos())
								.then(argument("dimension", word())
										.executes(context -> setStartPoint(
												context.getSource(),
												BlockPosArgument.getBlockPos(context, "pos"),
												getString(context, "dimension")
										))))
		));
	}

	private static int setStartPoint(CommandSourceStack source, String scenario, BlockPos pos, String dimension) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ResourceKey<Level> dimensionKey = dimensionKey(dimension);
		String scenarioId = PracticeStartPointManager.scenarioId(scenario);
		if (scenarioId == null) {
			throw INVALID_SCENARIO.create();
		}
		PracticeDestination destination = PracticeStartPointManager.setStartPoint(scenarioId, pos, dimensionKey);
		EndrRTAServerState.save(player.level().getServer());
		source.sendSuccess(() -> successMessage(destination, pos, dimensionKey), false);
		return 1;
	}

	private static int setStartPoint(CommandSourceStack source, BlockPos pos, String dimension) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ResourceKey<Level> dimensionKey = dimensionKey(dimension);
		try {
			PracticeDestination destination = PracticeStartPointManager.setSelectedStartPoint(player, pos, dimensionKey);
			EndrRTAServerState.save(player.level().getServer());
			source.sendSuccess(() -> successMessage(destination, pos, dimensionKey), false);
			return 1;
		} catch (IllegalStateException exception) {
			throw NO_SELECTED_SCENARIO.create();
		}
	}

	private static Component successMessage(PracticeDestination destination, BlockPos pos, ResourceKey<Level> dimensionKey) {
		return Component.literal("[EnderRTA] " + destination.label() + " のTP先を設定しました: "
				+ pos.getX() + " " + pos.getY() + " " + pos.getZ() + " / " + dimensionKey.identifier());
	}

	private static ResourceKey<Level> dimensionKey(String value) throws CommandSyntaxException {
		String dimensionId = switch (value.toLowerCase(Locale.ROOT)) {
			case "ow", "overworld" -> Identifier.DEFAULT_NAMESPACE + ":overworld";
			case "nether" -> Identifier.DEFAULT_NAMESPACE + ":the_nether";
			case "end" -> Identifier.DEFAULT_NAMESPACE + ":the_end";
			default -> throw INVALID_DIMENSION.create();
		};
		return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId));
	}
}
