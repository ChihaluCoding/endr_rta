package chihalu.endrrta.client.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public class EndrRTAOptionsScreen extends Screen {
	private static final int ROW_SPACING = 22;
	private static final int TAB_Y = 44;
	private static final int TAB_WIDTH = 76;
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_GAP = 4;
	private static final int CONTENT_Y = 78;
	private static final int SAVE_Y_OFFSET = 172;

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
			Button button = Button.builder(Component.literal(category.label()), ignored -> {
				selectedCategory = category;
				rebuildWidgets();
			}).bounds(x + i * (TAB_WIDTH + TAB_GAP), TAB_Y, TAB_WIDTH, TAB_HEIGHT).build();
			button.active = category != selectedCategory;
			addRenderableWidget(button);
		}
	}

	private void addCategorySettings(EndrRTAConfig config, int x, int y) {
		switch (selectedCategory) {
			case BASIC -> addBasicSettings(config, x, y);
			case PRACTICE -> addPracticeSettings(config, x, y);
			case HUD -> addHudSettings(config, x, y);
			case RESET -> addResetSettings(config, x, y);
		}
	}

	private void addBasicSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "練習モード", () -> config.practiceMode, value -> config.practiceMode = value);
		addToggle(x + 160, y, "HUD表示", () -> config.showHud, value -> config.showHud = value);
		y += ROW_SPACING;
		addToggle(x, y, "自動スプリット", () -> config.autoSplits, value -> config.autoSplits = value);
	}

	private void addPracticeSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "村へ初期移動", () -> config.forceVillageSpawn, value -> config.forceVillageSpawn = value);
		addToggle(x + 160, y, "鉱石を自動精錬", () -> config.autoSmeltOres, value -> config.autoSmeltOres = value);
		y += ROW_SPACING;
		addToggle(x, y, "エンダーアイ保護", () -> config.unbreakableEnderEyes, value -> config.unbreakableEnderEyes = value);
		addToggle(x + 160, y, "レーダー", () -> config.showRadar, value -> config.showRadar = value);
		y += ROW_SPACING;
		addToggle(x, y, "ベッド爆破支援", () -> config.showBedBlastAssist, value -> config.showBedBlastAssist = value);
	}

	private void addHudSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "座標換算", () -> config.showCoordinateConversion, value -> config.showCoordinateConversion = value);
		addToggle(x + 160, y, "バイオーム", () -> config.showBiome, value -> config.showBiome = value);
		y += ROW_SPACING;
		addToggle(x, y, "明るさ", () -> config.showLightLevel, value -> config.showLightLevel = value);
		addToggle(x + 160, y, "クリスタル数", () -> config.showCrystalCount, value -> config.showCrystalCount = value);
	}

	private void addResetSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "リセット確認", () -> config.confirmQuickReset, value -> config.confirmQuickReset = value);
		addToggle(x + 160, y, "プレビュー予約", () -> config.enableWorldPreview, value -> config.enableWorldPreview = value);
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
		graphics.centeredText(this.font, Component.literal(selectedCategory.description()), this.width / 2, 68, SUBTITLE);
	}

	private void drawPanel(@NonNull GuiGraphicsExtractor graphics) {
		int panelX = this.width / 2 - 174;
		int panelY = panelY();
		int panelWidth = 348;
		int panelHeight = 204;
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
		PRACTICE("練習補助", "練習用の補助機能を設定します"),
		HUD("HUD", "HUD に出す情報を設定します"),
		RESET("リセット", "リセットと予約機能を設定します");

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
}
