package chihalu.endrrta.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.projectile.EyeOfEnder;

@Mixin(EyeOfEnder.class)
public interface EyeOfEnderAccess {
	@Accessor("life")
	int endrrta$lifetime();
}
