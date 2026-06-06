package chihalu.endrrta.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.server.EndrRTAServerState;

@Mixin(EnderDragon.class)
public class EnderDragonMixin {
	@Inject(method = "tickDeath()V", at = @At("HEAD"))
	private void endrrta$stopTimerOnDeathEffect(CallbackInfo info) {
		Entity self = (Entity) (Object) this;
		if (self.level() instanceof ServerLevel level) {
			EndrRTAServerState.stopForDragon(level);
		}
	}
}
