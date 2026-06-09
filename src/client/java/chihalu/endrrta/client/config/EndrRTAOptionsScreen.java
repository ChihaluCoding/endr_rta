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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class EndrRTAOptionsScreen extends Screen {
	private static final int ROW_SPACING = 22;
	private static final int TAB_COLUMNS = 4;
	private static final int TAB_Y = 42;
	private static final int TAB_WIDTH = 76;
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_GAP = 4;
	private static final int TAB_ROW_GAP = 4;
	private static final int SUB_TAB_Y = 92;
	private static final int SUB_TAB_WIDTH = 76;
	private static final int SUB_TAB_HEIGHT = 20;
	private static final int SUB_TAB_GAP = 4;
	private static final int CONTENT_Y = 132;
	private static final int SAVE_Y_OFFSET = 258;

	private static final int PANEL_TOP = 0xDD111722;
	private static final int PANEL_BOTTOM = 0xCC070B12;
	private static final int PANEL_BORDER = 0x88FFD166;
	private static final int PANEL_ACCENT = 0xFFFFD166;
	private static final int TITLE = 0xFFFFFFFF;
	private static final int SUBTITLE = 0xFFB8C3CF;
	private static final String[] PRACTICE_SCENARIO_IDS = {
			"none",
			"nether_fortress",
			"bastion",
			"warped_forest_pearls",
			"lava_pool_portal",
			"ender_dragon"
	};

	private final @Nullable Screen parent;
	private SettingsCategory selectedCategory = SettingsCategory.BASIC;
	private SettingsSubcategory selectedSubcategory = SettingsSubcategory.GENERAL;

	public EndrRTAOptionsScreen(@Nullable Screen parent) {
		super(Component.literal("EnderRTA 設定"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		int x = this.width / 2 - 155;
		addCategoryTabs();
		addSubcategoryTabs();
		addSubcategorySettings(config, x, CONTENT_Y);
		addRenderableWidget(Button.builder(Component.literal("完了"), button -> {
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
				selectedSubcategory = SettingsSubcategory.firstFor(category);
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

	private void addSubcategoryTabs() {
		SettingsSubcategory[] subcategories = SettingsSubcategory.forCategory(selectedCategory);
		int totalWidth = subcategories.length * SUB_TAB_WIDTH + Math.max(0, subcategories.length - 1) * SUB_TAB_GAP;
		int x = this.width / 2 - totalWidth / 2;
		for (int i = 0; i < subcategories.length; i++) {
			SettingsSubcategory subcategory = subcategories[i];
			Button button = Button.builder(Component.literal(subcategory.label()), ignored -> {
				selectedSubcategory = subcategory;
				rebuildWidgets();
			}).bounds(
					x + i * (SUB_TAB_WIDTH + SUB_TAB_GAP),
					SUB_TAB_Y,
					SUB_TAB_WIDTH,
					SUB_TAB_HEIGHT
			).build();
			button.active = subcategory != selectedSubcategory;
			addRenderableWidget(button);
		}
	}

	private void addSubcategorySettings(EndrRTAConfig config, int x, int y) {
		switch (selectedSubcategory) {
			case GENERAL -> addGeneralSettings(config, x, y);
			case TIMER -> addTimerSettings(config, x, y);
			case PRACTICE_SCENARIO -> addPracticeScenarioSettings(config, x, y);
			case START_ASSIST -> addStartAssistSettings(config, x, y);
			case STRUCTURE_ASSIST -> addStructureAssistSettings(config, x, y);
			case ITEM_DROPS -> addItemDropSettings(config, x, y);
			case ITEM_PICKUP -> addItemPickupSettings(config, x, y);
			case EYE -> addEyeSettings(config, x, y);
			case ENDERMAN -> addEndermanSettings(config, x, y);
			case HUD_INFO -> addHudInfoSettings(config, x, y);
			case HUD_STRUCTURE -> addHudStructureSettings(config, x, y);
			case HUD_STYLE -> addHudStyleSettings(config, x, y);
			case HUD_RTA_VIEW -> addHudRtaViewSettings(x, y);
			case PIE_CONTROL -> addPieControlSettings(config, x, y);
			case RESET_CONFIRM -> addResetConfirmSettings(config, x, y);
			case WORLD_GENERATION -> addWorldGenerationSettings(config, x, y);
			case WORLD_RULES -> addWorldRulesSettings(config, x, y);
		}
	}

	private void addGeneralSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "練習モード", () -> config.practiceMode, value -> config.practiceMode = value);
		addToggle(x + 160, y, "HUD表示", () -> config.showHud, value -> config.showHud = value);
		y += ROW_SPACING;
		addToggle(x, y, "フルブライト", () -> config.fullBright, value -> config.fullBright = value);
	}

	private void addTimerSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "自動スプリット", () -> config.autoSplits, value -> config.autoSplits = value);
	}

	private void addPracticeScenarioSettings(EndrRTAConfig config, int x, int y) {
		addRenderableWidget(Button.builder(practiceScenarioMessage(config.practiceScenario), button -> {
			config.practiceScenario = nextPracticeScenario(config.practiceScenario);
			button.setMessage(practiceScenarioMessage(config.practiceScenario));
		}).bounds(x, y, 310, 20).build());
	}

	private void addStartAssistSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "村へ初期移動", () -> config.forceVillageSpawn, value -> config.forceVillageSpawn = value);
		addToggle(x + 160, y, "干草まとめ破壊", () -> config.breakNearbyHayBales, value -> config.breakNearbyHayBales = value);
		y += ROW_SPACING;
		addToggle(x, y, "初期チェスト", () -> config.placePracticeStartChest, value -> config.placePracticeStartChest = value);
	}

	private void addStructureAssistSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "要塞座標", () -> config.showRadar, value -> config.showRadar = value);
	}

	private void addItemDropSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "鉱石を自動精錬", () -> config.autoSmeltOres, value -> config.autoSmeltOres = value);
		addToggle(x + 160, y, "ブレイズロッド確定", () -> config.guaranteedBlazeRods, value -> config.guaranteedBlazeRods = value);
		y += ROW_SPACING;
		addToggle(x, y, "エンダーパール確定", () -> config.guaranteedEnderPearls, value -> config.guaranteedEnderPearls = value);
	}

	private void addItemPickupSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "指定アイテム拾得拒否", () -> config.preventIgnoredItemPickup, value -> config.preventIgnoredItemPickup = value);
		EditBox ignoredItems = new EditBox(
				this.font,
				x,
				y + ROW_SPACING,
				310,
				20,
				Component.literal("拒否アイテム")
		);
		ignoredItems.setMaxLength(2048);
		ignoredItems.setValue(String.join(", ", config.ignoredPickupItems));
		ignoredItems.setResponder(value -> config.ignoredPickupItems = parseIgnoredPickupItems(value));
		addRenderableWidget(ignoredItems);
	}

	private static String[] parseIgnoredPickupItems(String value) {
		return java.util.Arrays.stream(value.split("[,、\\s]+"))
				.map(String::trim)
				.filter(id -> !id.isEmpty())
				.map(EndrRTAOptionsScreen::resolveItemInput)
				.toArray(String[]::new);
	}

	private static String resolveItemInput(String input) {
		if (input.indexOf(':') >= 0) {
			return input;
		}

		String normalizedInput = normalizeItemName(input);
		for (Item item : BuiltInRegistries.ITEM) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null) {
				continue;
			}
			if (normalizeItemName(itemPath(id)).equals(normalizedInput)
					|| normalizeItemName(item.getName(item.getDefaultInstance()).getString()).equals(normalizedInput)) {
				return id.toString();
			}
		}
		return input;
	}

	private static String itemPath(Identifier id) {
		String value = id.toString();
		int namespaceEnd = value.indexOf(':');
		return namespaceEnd >= 0 ? value.substring(namespaceEnd + 1) : value;
	}

	private static String normalizeItemName(String value) {
		return value.toLowerCase(java.util.Locale.ROOT)
				.replace("_", "")
				.replace(" ", "")
				.replace("　", "")
				.replace("-", "");
	}

	private static Component practiceScenarioMessage(String id) {
		return Component.literal("個別練習: " + practiceScenarioLabel(id));
	}

	private static String nextPracticeScenario(String current) {
		for (int i = 0; i < PRACTICE_SCENARIO_IDS.length; i++) {
			if (PRACTICE_SCENARIO_IDS[i].equals(current)) {
				return PRACTICE_SCENARIO_IDS[(i + 1) % PRACTICE_SCENARIO_IDS.length];
			}
		}
		return PRACTICE_SCENARIO_IDS[0];
	}

	private static String practiceScenarioLabel(String id) {
		return switch (id) {
			case "nether_fortress" -> "ネザー要塞";
			case "bastion" -> "ピグリン要塞";
			case "warped_forest_pearls" -> "歪んだ森エンパ";
			case "lava_pool_portal" -> "マグマ溜まりゲート";
			case "ender_dragon" -> "エンドラ討伐";
			default -> "なし";
		};
	}

	private void addEyeSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "エンダーアイ保護", () -> config.unbreakableEnderEyes, value -> config.unbreakableEnderEyes = value);
	}

	private void addEndermanSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "エンダーマンブロック持ち禁止", () -> config.preventEndermanBlockCarry, value -> config.preventEndermanBlockCarry = value);
		addToggle(x + 160, y, "青森ボート誘導", () -> config.guideEndermenToBoats, value -> config.guideEndermenToBoats = value);
	}

	private void addHudInfoSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "座標換算", () -> config.showCoordinateConversion, value -> config.showCoordinateConversion = value);
		addToggle(x + 160, y, "バイオーム", () -> config.showBiome, value -> config.showBiome = value);
		y += ROW_SPACING;
		addToggle(x, y, "明るさ", () -> config.showLightLevel, value -> config.showLightLevel = value);
		addToggle(x + 160, y, "残りクリスタル数", () -> config.showCrystalCount, value -> config.showCrystalCount = value);
	}

	private void addHudStructureSettings(EndrRTAConfig config, int x, int y) {
		y += ROW_SPACING;
		addToggle(x, y, "ピグリン要塞タイプ", () -> config.showBastionType, value -> config.showBastionType = value);
		addRenderableWidget(new RangeSlider(
			x + 160,
			y,
			150,
			20,
			"要塞表示距離",
			config.structureFoundDistance,
			0,
			512,
			value -> config.structureFoundDistance = value
		));
		y += ROW_SPACING;
		addToggle(x, y, "要塞青森座標", () -> config.showFortressWarpedDistance, value -> config.showFortressWarpedDistance = value);
	}

	private void addHudStyleSettings(EndrRTAConfig config, int x, int y) {
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

	private void addHudRtaViewSettings(int x, int y) {
		Button renderDistanceInfo = Button.builder(Component.literal("描画距離: 2〜32を1ずつ増減"), ignored -> {
		}).bounds(x, y, 310, 20).build();
		renderDistanceInfo.active = false;
		addRenderableWidget(renderDistanceInfo);
	}

	private void addPieControlSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "円グラフ補助", () -> config.enablePieChartAssist, value -> config.enablePieChartAssist = value);
	}

	private void addResetConfirmSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "リセット確認", () -> config.confirmQuickReset, value -> config.confirmQuickReset = value);
		addToggle(x + 160, y, "前ワールドを残す", () -> config.resetKeepPreviousWorld, value -> config.resetKeepPreviousWorld = value);
	}

	private void addWorldGenerationSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "構造物を生成", () -> config.resetGenerateStructures, value -> config.resetGenerateStructures = value);
		addToggle(x + 160, y, "ボーナスチェスト", () -> config.resetBonusChest, value -> config.resetBonusChest = value);
	}

	private void addWorldRulesSettings(EndrRTAConfig config, int x, int y) {
		addToggle(x, y, "チートを許可", () -> config.resetAllowCommands, value -> config.resetAllowCommands = value);
		addToggle(x + 160, y, "ハードコア", () -> config.resetHardcore, value -> config.resetHardcore = value);
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
		graphics.centeredText(this.font, Component.literal(selectedSubcategory.description()), this.width / 2, 120, SUBTITLE);
	}

	private void drawPanel(@NonNull GuiGraphicsExtractor graphics) {
		int panelX = this.width / 2 - 174;
		int panelY = panelY();
		int panelWidth = 348;
		int panelHeight = 292;
		graphics.fill(0, 0, this.width, this.height, 0x88000000);
		graphics.fillGradient(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_TOP, PANEL_BOTTOM);
		graphics.outline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER);
		graphics.fill(panelX, panelY, panelX + 4, panelY + panelHeight, PANEL_ACCENT);

		// カテゴリボタンの2段目の下に区切り線を描画
		int lineY = TAB_Y + (TAB_HEIGHT + TAB_ROW_GAP) * 2 - 2; // 2段目の下あたり
		graphics.fill(panelX + 8, lineY, panelX + panelWidth - 8, lineY + 1, PANEL_BORDER);
	}

	private int panelY() {
		return 32;
	}

	private enum SettingsCategory {
		BASIC("基本"),
		START("初期補助"),
		DROPS("ドロップ"),
		ENDER("エンダー系"),
		HUD("HUD"),
		PIE("円グラフ"),
		RESET("リセット"),
		WORLD("ワールド");

		private final String label;

		SettingsCategory(String label) {
			this.label = label;
		}

		private String label() {
			return label;
		}
	}

	private enum SettingsSubcategory {
		GENERAL(SettingsCategory.BASIC, "基本", "モードと基本表示を設定します"),
		TIMER(SettingsCategory.BASIC, "タイマー", "自動スプリットを設定します"),
		PRACTICE_SCENARIO(SettingsCategory.BASIC, "個別練習", "練習する区間を選びます"),
		START_ASSIST(SettingsCategory.START, "開始補助", "開始直後の補助を設定します"),
		STRUCTURE_ASSIST(SettingsCategory.START, "構造物", "構造物探索の補助を設定します"),
		ITEM_DROPS(SettingsCategory.DROPS, "ドロップ", "練習用ドロップを設定します"),
		ITEM_PICKUP(SettingsCategory.DROPS, "拾得", "拾得制限を設定します"),
		EYE(SettingsCategory.ENDER, "エンダーアイ", "エンダーアイの補助を設定します"),
		ENDERMAN(SettingsCategory.ENDER, "エンダーマン", "エンダーマンの挙動を設定します"),
		HUD_INFO(SettingsCategory.HUD, "情報", "HUDに出す情報を設定します"),
		HUD_STRUCTURE(SettingsCategory.HUD, "構造物", "HUDの構造物表示を設定します"),
		HUD_STYLE(SettingsCategory.HUD, "見た目", "HUDの見た目を設定します"),
		HUD_RTA_VIEW(SettingsCategory.HUD, "RTA表示", "境界線と描画距離キーを確認します"),
		PIE_CONTROL(SettingsCategory.PIE, "操作補助", "円グラフ操作補助を設定します"),
		RESET_CONFIRM(SettingsCategory.RESET, "確認", "リセット確認を設定します"),
		WORLD_GENERATION(SettingsCategory.WORLD, "生成", "シードリセット時の生成設定です"),
		WORLD_RULES(SettingsCategory.WORLD, "ルール", "シードリセット時のワールドルールです");

		private final SettingsCategory category;
		private final String label;
		private final String description;

		SettingsSubcategory(SettingsCategory category, String label, String description) {
			this.category = category;
			this.label = label;
			this.description = description;
		}

		private static SettingsSubcategory firstFor(SettingsCategory category) {
			for (SettingsSubcategory subcategory : values()) {
				if (subcategory.category == category) {
					return subcategory;
				}
			}
			return GENERAL;
		}

		private static SettingsSubcategory[] forCategory(SettingsCategory category) {
			return java.util.Arrays.stream(values())
					.filter(subcategory -> subcategory.category == category)
					.toArray(SettingsSubcategory[]::new);
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

	private static final class RangeSlider extends AbstractSliderButton {
		private final String label;
		private final int min;
		private final int max;
		private final java.util.function.Consumer<Integer> setter;

		private RangeSlider(int x, int y, int width, int height, String label, int initialValue, int min, int max, java.util.function.Consumer<Integer> setter) {
			super(x, y, width, height, Component.empty(), Math.clamp((initialValue - min) / (double) Math.max(1, max - min), 0.0D, 1.0D));
			this.label = label;
			this.min = min;
			this.max = max;
			this.setter = setter;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(label + ": " + valueAsInt() + "m"));
		}

		@Override
		protected void applyValue() {
			setter.accept(valueAsInt());
		}

		private int valueAsInt() {
			int v = (int) Math.round(this.value * (max - min) + min);
			return Math.clamp(v, min, max);
		}
	}

}
