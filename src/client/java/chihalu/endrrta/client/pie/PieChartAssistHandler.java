package chihalu.endrrta.client.pie;

import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;
import org.lwjgl.glfw.GLFW;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import chihalu.endrrta.client.mixin.ProfilerPieChartAccessor;

public final class PieChartAssistHandler {
	private static final int ROOT_RETURN_STEPS = 12;
	private static final long MIDDLE_HOLD_NANOS = 350_000_000L;
	private static int selectedIndex = 1;
	private static long middlePressedAtNanos = -1L;

	private PieChartAssistHandler() {
	}

	public static void handleKeys(Minecraft minecraft, KeyMapping backKey, KeyMapping rootKey) {
		while (backKey.consumeClick()) {
			if (assistEnabled(minecraft)) {
				moveBack(minecraft);
			}
		}
		while (rootKey.consumeClick()) {
			if (assistEnabled(minecraft)) {
				moveRoot(minecraft);
			}
		}
	}

	public static boolean shouldShowHint(Minecraft minecraft) {
		return active(minecraft);
	}

	public static boolean handleScroll(Minecraft minecraft, double verticalAmount) {
		if (!active(minecraft) || verticalAmount == 0.0D) {
			return false;
		}
		int direction = verticalAmount > 0.0D ? -1 : 1;
		selectedIndex = clampSelectedIndex(minecraft, selectedIndex + direction);
		showMessage(minecraft, "選択: " + selectedLabel(minecraft));
		return true;
	}

	public static boolean handleMouseButton(Minecraft minecraft, int button, int action) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
			return handleMiddleButton(minecraft, action);
		}
		if (!active(minecraft) || action != GLFW.GLFW_PRESS) {
			return false;
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return selectCurrent(minecraft);
		}
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			moveBack(minecraft);
			return true;
		}
		return false;
	}

	public static String selectedLabel(Minecraft minecraft) {
		List<ResultField> rows = profilerRows(minecraft);
		if (rows.size() <= 1) {
			return "なし";
		}
		int row = clampSelectedIndex(minecraft, selectedIndex);
		ResultField field = rows.get(row);
		return "[" + row + "] " + field.name;
	}

	public static boolean selectByIndex(Minecraft minecraft, int index) {
		if (!assistEnabled(minecraft) || !minecraft.getDebugOverlay().showProfilerChart() || index < 0) {
			return false;
		}
		minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(index);
		selectedIndex = 1;
		return true;
	}

	public static void moveBack(Minecraft minecraft) {
		if (!minecraft.getDebugOverlay().showProfilerChart()) {
			return;
		}
		minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(0);
		selectedIndex = 1;
		showMessage(minecraft, "円グラフを1つ戻しました。");
	}

	public static void moveRoot(Minecraft minecraft) {
		if (!minecraft.getDebugOverlay().showProfilerChart()) {
			return;
		}
		for (int i = 0; i < ROOT_RETURN_STEPS; i++) {
			minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(0);
		}
		selectedIndex = 1;
		showMessage(minecraft, "円グラフを root に戻しました。");
	}

	public static void closeAssist(Minecraft minecraft) {
		hideProfilerChart(minecraft);
	}

	public static void hideProfilerChart(Minecraft minecraft) {
		DebugScreenOverlay overlay = minecraft.getDebugOverlay();
		if (overlay.showProfilerChart()) {
			overlay.toggleProfilerChart();
		}
	}

	private static boolean assistEnabled(Minecraft minecraft) {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		return minecraft.player != null
				&& minecraft.level != null
				&& minecraft.screen == null
				&& config.allowsPracticeAssist()
				&& config.enablePieChartAssist;
	}

	private static boolean active(Minecraft minecraft) {
		return assistEnabled(minecraft) && minecraft.getDebugOverlay().showProfilerChart();
	}

	private static boolean handleMiddleButton(Minecraft minecraft, int action) {
		if (!assistEnabled(minecraft)) {
			middlePressedAtNanos = -1L;
			return false;
		}
		if (action == GLFW.GLFW_PRESS) {
			middlePressedAtNanos = System.nanoTime();
			return false;
		}
		if (action != GLFW.GLFW_RELEASE || middlePressedAtNanos < 0L) {
			return false;
		}

		long heldNanos = System.nanoTime() - middlePressedAtNanos;
		middlePressedAtNanos = -1L;
		if (heldNanos < MIDDLE_HOLD_NANOS) {
			return active(minecraft);
		}

		togglePieChart(minecraft);
		return true;
	}

	private static void togglePieChart(Minecraft minecraft) {
		DebugScreenOverlay overlay = minecraft.getDebugOverlay();
		overlay.toggleProfilerChart();
		if (overlay.showProfilerChart()) {
			selectedIndex = clampSelectedIndex(minecraft, selectedIndex);
			showMessage(minecraft, "円グラフを表示しました。ホイールで選択、左クリックで決定できます。");
		} else {
			showMessage(minecraft, "円グラフを非表示にしました。");
		}
	}

	private static boolean selectCurrent(Minecraft minecraft) {
		selectedIndex = clampSelectedIndex(minecraft, selectedIndex);
		boolean selected = selectByIndex(minecraft, selectedIndex);
		if (selected) {
			showMessage(minecraft, "選択しました。");
		}
		return selected;
	}

	private static int clampSelectedIndex(Minecraft minecraft, int index) {
		int rowCount = Math.max(1, profilerRows(minecraft).size() - 1);
		if (index < 1) {
			return rowCount;
		}
		if (index > rowCount) {
			return 1;
		}
		return index;
	}

	private static List<ResultField> profilerRows(Minecraft minecraft) {
		if (!minecraft.getDebugOverlay().showProfilerChart()) {
			return List.of();
		}
		ProfilerPieChart chart = minecraft.getDebugOverlay().getProfilerPieChart();
		ProfilerPieChartAccessor accessor = (ProfilerPieChartAccessor) chart;
		ProfileResults results = accessor.endrrta$profilerPieChartResults();
		if (results == null) {
			return List.of();
		}
		return results.getTimes(accessor.endrrta$profilerTreePath());
	}

	private static void showMessage(Minecraft minecraft, String message) {
		minecraft.gui.setOverlayMessage(Component.literal("[EnderRTA] " + message), false);
	}
}
