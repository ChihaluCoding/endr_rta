package chihalu.endrrta.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.server.IgnoredPickupItems;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Shadow
	public abstract ItemStack getItem();

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void endrrta$preventIgnoredPickup(Player player, CallbackInfo info) {
		if (IgnoredPickupItems.shouldIgnore(getItem())) {
			info.cancel();
		}
	}
}
