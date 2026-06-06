package chihalu.endrrta.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.projectile.EyeOfEnder;

@Mixin(value = EyeOfEnder.class, remap = false)
public interface EyeOfEnderAccess {
	@Accessor(value = "life", remap = false)
	int endrrta$lifetime();
}
