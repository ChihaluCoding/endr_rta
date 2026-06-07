package chihalu.endrrta.client.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerAccessor {
	@Invoker("handleDebugKeys")
	boolean endrrta$handleDebugKeys(KeyEvent event);
}
