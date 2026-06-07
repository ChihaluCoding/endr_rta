package chihalu.endrrta.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import chihalu.endrrta.EndrRTA;
import chihalu.endrrta.client.config.EndrRTAModMenu;
import chihalu.endrrta.client.hud.EndrRTAHud;
import chihalu.endrrta.client.pie.PieChartAssistHandler;
import chihalu.endrrta.client.reset.QuickResetHandler;
import chihalu.endrrta.client.view.RtaViewAssistHandler;
import chihalu.endrrta.config.EndrRTAConfigManager;

public class EndrRTAClient implements ClientModInitializer {
	private static KeyMapping quickResetKey;
	private static KeyMapping configKey;
	private static KeyMapping pieChartBackKey;
	private static KeyMapping pieChartRootKey;
	private static KeyMapping chunkBordersKey;
	private static KeyMapping hitboxesKey;
	private static KeyMapping renderDistanceIncreaseKey;
	private static KeyMapping renderDistanceDecreaseKey;
	private static boolean narratorHotkeyDisabled;

	@Override
	public void onInitializeClient() {
		EndrRTAConfigManager.load();
		quickResetKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.quick_reset",
				GLFW.GLFW_KEY_R,
				KeyMapping.Category.MISC
		));
		configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.config",
				GLFW.GLFW_KEY_O,
				KeyMapping.Category.MISC
		));
		pieChartBackKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.pie_chart_back",
				GLFW.GLFW_KEY_BACKSPACE,
				KeyMapping.Category.MISC
		));
		pieChartRootKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.pie_chart_root",
				GLFW.GLFW_KEY_HOME,
				KeyMapping.Category.MISC
		));
		chunkBordersKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.chunk_borders_quick",
				GLFW.GLFW_KEY_Z,
				KeyMapping.Category.MISC
		));
		hitboxesKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.hitboxes_toggle",
				GLFW.GLFW_KEY_C,
				KeyMapping.Category.MISC
		));
		renderDistanceIncreaseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.render_distance_increase",
				GLFW.GLFW_KEY_V,
				KeyMapping.Category.MISC
		));
		renderDistanceDecreaseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.endrrta.render_distance_decrease",
				GLFW.GLFW_KEY_B,
				KeyMapping.Category.MISC
		));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			disableNarratorHotkeyWhenReady(client);
			while (quickResetKey.consumeClick()) {
				QuickResetHandler.requestQuickReset(client);
			}
			while (configKey.consumeClick()) {
				client.setScreen(EndrRTAModMenu.createConfigScreen(client.screen));
			}
			PieChartAssistHandler.handleKeys(client, pieChartBackKey, pieChartRootKey);
			RtaViewAssistHandler.handleKeys(client, chunkBordersKey, hitboxesKey, renderDistanceIncreaseKey, renderDistanceDecreaseKey);
		});
		HudElementRegistry.addLast(Identifier.parse(EndrRTA.MOD_ID + ":hud"), EndrRTAHud::render);
	}

	private static void disableNarratorHotkeyWhenReady(Minecraft client) {
		if (narratorHotkeyDisabled || client.options == null) {
			return;
		}
		if (Boolean.TRUE.equals(client.options.narratorHotkey().get())) {
			client.options.narratorHotkey().set(false);
			client.options.save();
		}
		narratorHotkeyDisabled = true;
	}
}
