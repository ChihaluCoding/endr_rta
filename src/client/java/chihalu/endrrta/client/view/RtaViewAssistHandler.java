package chihalu.endrrta.client.view;

import chihalu.endrrta.client.mixin.KeyboardHandlerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class RtaViewAssistHandler {
	private static final int MIN_RENDER_DISTANCE = 2;
	private static final int MAX_RENDER_DISTANCE = 32;

	private RtaViewAssistHandler() {
	}

	public static void handleKeys(Minecraft minecraft, KeyMapping chunkBordersKey, KeyMapping hitboxesKey, KeyMapping renderDistanceIncreaseKey, KeyMapping renderDistanceDecreaseKey) {
		while (chunkBordersKey.consumeClick()) {
			toggleChunkBorders(minecraft);
		}
		while (hitboxesKey.consumeClick()) {
			toggleHitboxes(minecraft);
		}
		while (renderDistanceIncreaseKey.consumeClick()) {
			changeRenderDistance(minecraft, 1);
		}
		while (renderDistanceDecreaseKey.consumeClick()) {
			changeRenderDistance(minecraft, -1);
		}
	}

	private static void toggleChunkBorders(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.keyboardHandler == null) {
			return;
		}
		boolean handled = ((KeyboardHandlerAccessor) minecraft.keyboardHandler)
				.endrrta$handleDebugKeys(new KeyEvent(GLFW.GLFW_KEY_G, 0, 0));
		if (!handled) {
			minecraft.player.sendSystemMessage(Component.literal("[EnderRTA] チャンク境界線を切り替えられませんでした。"));
		}
	}

	private static void toggleHitboxes(Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		boolean enabled = minecraft.debugEntries.toggleStatus(DebugScreenEntries.ENTITY_HITBOXES);
		minecraft.debugEntries.save();
		minecraft.player.sendSystemMessage(Component.literal("[EnderRTA] 当たり判定: " + (enabled ? "オン" : "オフ")));
	}

	private static void changeRenderDistance(Minecraft minecraft, int delta) {
		if (minecraft.player == null) {
			return;
		}

		int current = (Integer) minecraft.options.renderDistance().get();
		int next = current + delta;
		if (next > MAX_RENDER_DISTANCE) {
			next = MIN_RENDER_DISTANCE;
		} else if (next < MIN_RENDER_DISTANCE) {
			next = MAX_RENDER_DISTANCE;
		}

		minecraft.options.renderDistance().set(next);
		minecraft.options.save();
		minecraft.player.sendSystemMessage(Component.literal("[EnderRTA] 描画距離: " + next + "チャンク"));
	}
}
