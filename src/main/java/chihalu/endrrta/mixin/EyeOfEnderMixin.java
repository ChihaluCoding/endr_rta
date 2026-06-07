package chihalu.endrrta.mixin;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import chihalu.endrrta.config.EndrRTAConfigManager;

@Mixin(value = EyeOfEnder.class, remap = false)
public abstract class EyeOfEnderMixin {
	@Unique
	private UUID endrrta$throwerId;

	@Shadow(remap = false)
	public abstract ItemStack getItem();

	@SuppressWarnings("unused")
	@Inject(method = "tick()V", at = @At("HEAD"), cancellable = true, remap = false)
	private void endrrta$forceDropOnFinish(CallbackInfo info) {
		if (!EndrRTAConfigManager.get().allowsUnbreakableEnderEyes()) {
			return;
		}

		Entity self = (Entity) (Object) this;
		if (!(self.level() instanceof ServerLevel level) || self.isRemoved()) {
			return;
		}
		if (endrrta$throwerId == null) {
			endrrta$throwerId = endrrta$findNearestThrower(level, self);
		}

		EyeOfEnderAccess access = (EyeOfEnderAccess) this;
		if (access.endrrta$lifetime() < 80) {
			return;
		}

		ItemStack stack = getItem().isEmpty() ? new ItemStack(Items.ENDER_EYE) : getItem().copyWithCount(1);
		ServerPlayer owner = endrrta$findOwner(level, self);
		if (owner != null && !owner.addItem(stack)) {
			owner.drop(stack, false);
		}
		self.discard();
		info.cancel();
	}

	@Unique
	private UUID endrrta$findNearestThrower(ServerLevel level, Entity self) {
		ServerPlayer nearestPlayer = null;
		double nearestDistance = 64.0D;
		for (ServerPlayer player : level.players()) {
			double distance = player.distanceToSqr(self);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestPlayer = player;
			}
		}
		return nearestPlayer == null ? null : nearestPlayer.getUUID();
	}

	@Unique
	private ServerPlayer endrrta$findOwner(ServerLevel level, Entity self) {
		if (endrrta$throwerId != null) {
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(endrrta$throwerId);
			if (owner != null) {
				return owner;
			}
		}

		UUID fallbackId = endrrta$findNearestThrower(level, self);
		if (fallbackId == null) {
			return null;
		}
		return level.getServer().getPlayerList().getPlayer(fallbackId);
	}
}
