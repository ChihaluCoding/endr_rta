package chihalu.endrrta.server;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class PracticeMenuBook {
	private static final String MENU_BOOK_NAME = "メニュー";

	private PracticeMenuBook() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!isMenuBook(stack)) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				openLauncher(serverPlayer);
			}
			return InteractionResult.SUCCESS;
		});
	}

	public static void giveIfNeeded(ServerPlayer player) {
		if (!isPracticeHubSession()) {
			return;
		}
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (isMenuBook(inventory.getItem(slot))) {
				return;
			}
		}
		if (!inventory.add(createMenuBook())) {
			player.drop(createMenuBook(), false);
		}
	}

	private static void openLauncher(ServerPlayer player) {
		Map<String, PracticeDestination> destinations = new HashMap<>(PracticeStartPointManager.destinations());
		PracticeSelectionMenu.openLauncher(player, destinations);
		if (destinations.isEmpty()) {
			player.sendSystemMessage(Component.literal("[EnderRTA] TP先が未設定です。メニュー項目をクリックしてから /setstartpoint x y z ow/nether/end で設定してください。"));
		}
	}

	private static boolean isPracticeHubSession() {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		return config.practiceMode && "none".equals(config.practiceScenario);
	}

	private static ItemStack createMenuBook() {
		ItemStack stack = new ItemStack(Items.BOOK);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(MENU_BOOK_NAME));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	private static boolean isMenuBook(ItemStack stack) {
		if (!stack.is(Items.BOOK) || !stack.has(DataComponents.CUSTOM_NAME)) {
			return false;
		}
		return MENU_BOOK_NAME.equals(stack.getHoverName().getString());
	}
}
