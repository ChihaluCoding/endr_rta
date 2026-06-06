package chihalu.endrrta.client.config;

import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public final class EndrRTAModMenu {
	private EndrRTAModMenu() {
	}

	public static Screen createConfigScreen(@Nullable Screen parent) {
		return new EndrRTAOptionsScreen(parent);
	}
}
