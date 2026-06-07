package chihalu.endrrta.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.config.EndrRTAConfigManager;

@Mixin(value = EnderMan.class, remap = false)
public class EnderManMixin {
	@Inject(method = "setCarriedBlock(Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true, remap = false)
	private void endrrta$preventCarriedBlock(@Nullable BlockState blockState, CallbackInfo info) {
		if (blockState != null && shouldPreventBlockCarry()) {
			info.cancel();
		}
	}

	@Inject(method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"), remap = false)
	private void endrrta$clearCarriedBlock(ServerLevel level, CallbackInfo info) {
		if (!shouldPreventBlockCarry()) {
			return;
		}
		EnderMan self = (EnderMan) (Object) this;
		if (self.getCarriedBlock() != null) {
			self.setCarriedBlock(null);
		}
	}

	private static boolean shouldPreventBlockCarry() {
		return EndrRTAConfigManager.get().allowsPreventEndermanBlockCarry();
	}
}
