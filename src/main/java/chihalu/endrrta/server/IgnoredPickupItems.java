package chihalu.endrrta.server;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class IgnoredPickupItems {
	private IgnoredPickupItems() {
	}

	public static boolean shouldIgnore(ItemStack stack) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		if (!config.allowsPracticeAssist() || !config.preventIgnoredItemPickup || stack.isEmpty()) {
			return false;
		}

		Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (itemId == null) {
			return false;
		}
		return ignoredIds(config).contains(itemId.toString());
	}

	private static Set<String> ignoredIds(EndrRTAConfig config) {
		Set<String> ids = new HashSet<>();
		for (String rawId : config.ignoredPickupItems) {
			if (rawId != null && !rawId.isBlank()) {
				ids.add(rawId.trim());
			}
		}
		return ids;
	}
}
