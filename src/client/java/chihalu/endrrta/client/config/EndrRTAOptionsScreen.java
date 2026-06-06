package chihalu.endrrta.client.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EndrRTAOptionsScreen extends Screen {
	private static final int ROW_SPACING = 22;
	private static final int TAB_COLUMNS = 4;
	private static final int TAB_Y = 42;
	private static final int TAB_WIDTH = 76;
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_GAP = 4;
	private static final int TAB_ROW_GAP = 4;
	private static final int CONTENT_Y = 100;
	private static final int SAVE_Y_OFFSET = 214;

	private static final int PANEL_TOP = 0xDD111722;
	private static final int PANEL_BOTTOM = 0xCC070B12;
	private static final int PANEL_BORDER = 0x88FFD166;
	private static final int PANEL_ACCENT = 0xFFFFD166;
	private static final int TITLE = 0xFFFFFFFF;
	private static final int SUBTITLE = 0xFFB8C3CF;

	private final @Nullable Screen parent;
	private SettingsCategory selectedCategory = SettingsCategory.BASIC;

	public EndrRTAOptionsScreen(@Nullable Screen parent) {
		super(Component.literal("EndrRTA 設定"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		int x = this.width / 2 - 155;
		addCategoryTabs();
		addCategorySettings(config, x, CONTENT_Y);
		addRenderableWidget(Button.builder(Component.literal("保存して戻る"), button -> {
			saveAndReturn();
		}).bounds(this.width / 2 - 100, panelY() + SAVE_Y_OFFSET, 200, 20).build());
	}

	private void addCategoryTabs() {
		int x = this.width / 2 - 158;
		SettingsCategory[] categories = SettingsCategory.values();
		for (int i = 0; i < categories.length; i++) {
			SettingsCategory category = categories[i];
			int column = i % TAB_COLUMNS;
			int row = i / TAB_COLUMNS;
			Button button = Button.builder(Component.literal(category.label()), ignored -> {
				selectedCategory = category;
				rebuildWidgets();
			}).bounds(
					x + column * (TAB_WIDTH + TAB_GAP),
					TAB_Y + row * (TAB_HEIGHT + TAB_ROW_GAP),
					TAB_WIDTH,
					TAB_HEIGHT
			).build();
			button.active = category != selectedCategory;
			addRenderableWidget(button);
		}
	}

	private void addCategorySettings(EndrRTAConfig config, int x, int y) {
		switch (selectedCategory) {
			case BASIC -> addBasicSettings(config, x, y);
			case START -> addStartSettings(config, x, y);
			case DROPS -> addDropSettings(config, x, y);
			case ENDER -> addEnderSettings(config, x, y);
			case HUD -> addHudSettings(config, x, y);
			case PIE -> addPieSettings(config, x, y);
			case RESET -> addResetSettings(config, x, y);
			case WORLD -> addWorldSettings(config, x, y);
		}
	}

	private void addBasicSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "練習モード", () -> config.practiceMode, value -> config.practiceMode = value);
		addToggle(x + 160, y, "HUD表示", () -> config.showHud, value -> config.showHud = value);
		y += ROW_SPACING;
		addToggle(x, y, "自動スプリット", () -> config.autoSplits, value -> config.autoSplits = value);
	}

	private void addStartSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "村へ初期移動", () -> config.forceVillageSpawn, value -> config.forceVillageSpawn = value);
		addToggle(x + 160, y, "ベッド爆破支援", () -> config.showBedBlastAssist, value -> config.showBedBlastAssist = value);
		y += ROW_SPACING;
		addToggle(x, y, "レーダー", () -> config.showRadar, value -> config.showRadar = value);
		addToggle(x + 160, y, "干草まとめ破壊", () -> config.breakNearbyHayBales, value -> config.breakNearbyHayBales = value);
	}

	private void addDropSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "鉱石を自動精錬", () -> config.autoSmeltOres, value -> config.autoSmeltOres = value);
		addToggle(x + 160, y, "ブレイズロッド確定", () -> config.guaranteedBlazeRods, value -> config.guaranteedBlazeRods = value);
		y += ROW_SPACING;
		addToggle(x, y, "エンダーパール確定", () -> config.guaranteedEnderPearls, value -> config.guaranteedEnderPearls = value);
	}

	private void addEnderSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "エンダーアイ保護", () -> config.unbreakableEnderEyes, value -> config.unbreakableEnderEyes = value);
		addToggle(x + 160, y, "エンダーマンブロック持ち禁止", () -> config.preventEndermanBlockCarry, value -> config.preventEndermanBlockCarry = value);
	}

	private void addPieSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "円グラフ補助", () -> config.enablePieChartAssist, value -> config.enablePieChartAssist = value);
	}

	private void addWorldSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "構造物を生成", () -> config.resetGenerateStructures, value -> config.resetGenerateStructures = value);
		addToggle(x + 160, y, "ボーナスチェスト", () -> config.resetBonusChest, value -> config.resetBonusChest = value);
		y += ROW_SPACING;
		addToggle(x, y, "チートを許可", () -> config.resetAllowCommands, value -> config.resetAllowCommands = value);
		addToggle(x + 160, y, "ハードコア", () -> config.resetHardcore, value -> config.resetHardcore = value);
	}

	private void addHudSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "座標換算", () -> config.showCoordinateConversion, value -> config.showCoordinateConversion = value);
		addToggle(x + 160, y, "バイオーム", () -> config.showBiome, value -> config.showBiome = value);
		y += ROW_SPACING;
		addToggle(x, y, "明るさ", () -> config.showLightLevel, value -> config.showLightLevel = value);
		addToggle(x + 160, y, "残りクリスタル数", () -> config.showCrystalCount, value -> config.showCrystalCount = value);
		y += ROW_SPACING;
		addRenderableWidget(new PercentSlider(
				x,
				y,
				310,
				20,
				"HUD背景透明度",
				config.hudBackgroundOpacity,
				value -> config.hudBackgroundOpacity = value
		));
	}

	private void addResetSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "リセット確認", () -> config.confirmQuickReset, value -> config.confirmQuickReset = value);
	}

	private void addToggle(int x, int y, @NonNull String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		addRenderableWidget(CycleButton.onOffBuilder(getter.get()).create(
				x,
				y,
				150,
				20,
				Component.literal(label),
				(button, value) -> setter.accept(value)
		));
	}

	@Override
	public void onClose() {
		saveAndReturn();
	}

	private void saveAndReturn() {
		EndrRTAConfigManager.save();
		Minecraft minecraft = this.minecraft;
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		drawPanel(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 18, TITLE);
		graphics.centeredText(this.font, Component.literal(selectedCategory.description()), this.width / 2, 88, SUBTITLE);
	}

	private void drawPanel(@NonNull GuiGraphicsExtractor graphics) {
		int panelX = this.width / 2 - 174;
		int panelY = panelY();
		int panelWidth = 348;
		int panelHeight = 246;
		graphics.fill(0, 0, this.width, this.height, 0x88000000);
		graphics.fillGradient(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_TOP, PANEL_BOTTOM);
		graphics.outline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);
		graphics.fill(panelX, panelY, panelX + 4, panelY + panelHeight, PANEL_ACCENT);
	}

	private int panelY() {
		return 32;
	}

	private enum SettingsCategory {
		BASIC("基本", "モードと基本表示を設定します"),
		START("初期補助", "開始直後の補助を設定します"),
		DROPS("ドロップ", "練習用のドロップ補助を設定します"),
		ENDER("エンダーマン", "エンダーアイとエンダーマン補助を設定します"),
		HUD("HUD", "HUD に出す情報を設定します"),
		PIE("円グラフ", "円グラフ操作補助を設定します"),
		RESET("リセット", "リセット確認を設定します"),
		WORLD("ワールド", "シードリセット時のワールド設定です");

		private final String label;
		private final String description;

		SettingsCategory(String label, String description) {
			this.label = label;
			this.description = description;
		}

		private String label() {
			return label;
		}

		private String description() {
			return description;
		}
	}

	private static final class PercentSlider extends AbstractSliderButton {
		private final String label;
		private final Consumer<Integer> setter;

		private PercentSlider(int x, int y, int width, int height, String label, int initialValue, Consumer<Integer> setter) {
			super(x, y, width, height, Component.empty(), Math.clamp(initialValue, 0, 100) / 100.0D);
			this.label = label;
			this.setter = setter;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(label + ": " + percent() + "%"));
		}

		@Override
		protected void applyValue() {
			setter.accept(percent());
		}

		private int percent() {
			return Math.clamp((int) Math.round(value * 100.0D), 0, 100);
		}
	}
}
