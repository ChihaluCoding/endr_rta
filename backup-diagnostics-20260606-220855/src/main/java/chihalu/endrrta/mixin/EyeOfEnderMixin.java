package chihalu.endrrta.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.config.EndrRTAConfigManager;

@Mixin(EyeOfEnder.class)
public abstract class EyeOfEnderMixin {
	@Shadow
	public abstract ItemStack getItem();

	@Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
	private void endrrta$forceDropOnFinish(CallbackInfo info) {
		if (!EndrRTAConfigManager.get().allowsPracticeAssist() || !EndrRTAConfigManager.get().unbreakableEnderEyes) {
			return;
		}

		Entity self = (Entity) (Object) this;
		if (!(self.level() instanceof ServerLevel level) || self.isRemoved()) {
			return;
		}

		EyeOfEnderAccess access = (EyeOfEnderAccess) this;
		if (access.endrrta$lifetime() < 80) {
			return;
		}

		ItemStack stack = getItem().isEmpty() ? new ItemStack(Items.ENDER_EYE) : getItem().copyWithCount(1);
		self.spawnAtLocation(level, stack);
		self.discard();
		info.cancel();
	}
}
