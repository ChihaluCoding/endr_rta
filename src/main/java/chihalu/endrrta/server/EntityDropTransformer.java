package chihalu.endrrta.server;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public final class EntityDropTransformer {
	private EntityDropTransformer() {
	}

	public static void ensurePracticeDrops(Holder<LootTable> holder, LootContext context, List<ItemStack> drops) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (entity == null) {
			return;
		}
		if (config.allowsGuaranteedBlazeRods() && entity.getType() == EntityType.BLAZE) {
			ensureDrop(drops, Items.BLAZE_ROD);
		}
		if (config.allowsGuaranteedEnderPearls() && entity.getType() == EntityType.ENDERMAN) {
			ensureDrop(drops, Items.ENDER_PEARL);
		}
	}

	private static void ensureDrop(List<ItemStack> drops, Item item) {
		for (ItemStack drop : drops) {
			if (drop.getItem() == item && drop.getCount() > 0) {
				return;
			}
		}
		drops.add(new ItemStack(item));
	}
}
