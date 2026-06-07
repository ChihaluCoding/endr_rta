package chihalu.endrrta.client.inventory;

import java.util.LinkedHashSet;
import java.util.Set;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class IgnoredPickupItemRegistrar {
	private IgnoredPickupItemRegistrar() {
	}

	public static boolean registerHoveredStack(Minecraft minecraft, ItemStack stack) {
		if (minecraft.player == null || stack.isEmpty()) {
			return false;
		}

		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (itemId == null) {
			return false;
		}

		EndrRTAConfig config = EndrRTAConfigManager.get();
		Set<String> ignoredIds = new LinkedHashSet<>();
		for (String rawId : config.ignoredPickupItems) {
			if (rawId != null && !rawId.isBlank()) {
				ignoredIds.add(rawId.trim());
			}
		}

		String id = itemId.toString();
		String itemName = stack.getHoverName().getString();
		if (ignoredIds.remove(id)) {
			config.ignoredPickupItems = ignoredIds.toArray(String[]::new);
			EndrRTAConfigManager.save();
			minecraft.player.sendSystemMessage(Component.literal("[EnderRTA] " + itemName + " の拾得拒否を解除しました。"));
			return true;
		}

		ignoredIds.add(id);
		config.ignoredPickupItems = ignoredIds.toArray(String[]::new);
		EndrRTAConfigManager.save();
		minecraft.player.sendSystemMessage(Component.literal("[EnderRTA] " + itemName + " を拾得拒否に登録しました。"));
		return true;
	}
}
