package chihalu.endrrta.server;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import chihalu.endrrta.config.EndrRTAConfigManager;

public final class OreDropTransformer {
	private OreDropTransformer() {
	}

	public static void replaceRawOreDrops(Holder<LootTable> holder, LootContext context, List<ItemStack> drops) {
		if (!EndrRTAConfigManager.get().allowsPracticeAssist() || !EndrRTAConfigManager.get().autoSmeltOres) {
			return;
		}

		BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
		if (state == null || !isSmeltableOre(state)) {
			return;
		}

		for (int i = 0; i < drops.size(); i++) {
			ItemStack stack = drops.get(i);
			Item replacement = smeltedItem(stack.getItem());
			if (replacement != null) {
				drops.set(i, new ItemStack(replacement, stack.getCount()));
			}
		}
	}

	private static boolean isSmeltableOre(BlockState state) {
		return state.is(Blocks.IRON_ORE)
				|| state.is(Blocks.DEEPSLATE_IRON_ORE)
				|| state.is(Blocks.COPPER_ORE)
				|| state.is(Blocks.DEEPSLATE_COPPER_ORE)
				|| state.is(Blocks.GOLD_ORE)
				|| state.is(Blocks.DEEPSLATE_GOLD_ORE);
	}

	private static Item smeltedItem(Item item) {
		if (item == Items.RAW_IRON) {
			return Items.IRON_INGOT;
		}
		if (item == Items.RAW_COPPER) {
			return Items.COPPER_INGOT;
		}
		if (item == Items.RAW_GOLD) {
			return Items.GOLD_INGOT;
		}
		return null;
	}
}
