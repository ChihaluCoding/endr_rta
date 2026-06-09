package chihalu.endrrta.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = KeyboardHandler.class, remap = false)
public interface KeyboardHandlerAccessor {
	@Invoker(value = "handleDebugKeys", remap = false)
	boolean endrrta$handleDebugKeys(KeyEvent event);
}
