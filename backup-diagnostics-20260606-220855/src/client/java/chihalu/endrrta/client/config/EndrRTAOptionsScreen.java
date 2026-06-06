package chihalu.endrrta.client.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import chihalu.endrrta.config.EndrRTAConfig;
import chihalu.endrrta.config.EndrRTAConfigManager;

public class EndrRTAOptionsScreen extends Screen {
	private final Screen parent;

	public EndrRTAOptionsScreen(Screen parent) {
		super(Component.literal("EndrRTA 設定"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		EndrRTAConfig config = EndrRTAConfigManager.get();
		int x = this.width / 2 - 155;
		int y = 42;
		addToggle(x, y, "練習モード", () -> config.practiceMode, value -> config.practiceMode = value);
		addToggle(x + 160, y, "HUD", () -> config.showHud, value -> config.showHud = value);
		y += 24;
		addToggle(x, y, "村スポーン", () -> config.forceVillageSpawn, value -> config.forceVillageSpawn = value);
		addToggle(x + 160, y, "鉱石焼き不要", () -> config.autoSmeltOres, value -> config.autoSmeltOres = value);
		y += 24;
		addToggle(x, y, "エンダーアイ保護", () -> config.unbreakableEnderEyes, value -> config.unbreakableEnderEyes = value);
		addToggle(x + 160, y, "自動スプリット", () -> config.autoSplits, value -> config.autoSplits = value);
		y += 24;
		addToggle(x, y, "座標換算", () -> config.showCoordinateConversion, value -> config.showCoordinateConversion = value);
		addToggle(x + 160, y, "バイオーム", () -> config.showBiome, value -> config.showBiome = value);
		y += 24;
		addToggle(x, y, "ライト", () -> config.showLightLevel, value -> config.showLightLevel = value);
		addToggle(x + 160, y, "クリスタル数", () -> config.showCrystalCount, value -> config.showCrystalCount = value);
		y += 24;
		addToggle(x, y, "レーダー", () -> config.showRadar, value -> config.showRadar = value);
		addToggle(x + 160, y, "ベッド支援", () -> config.showBedBlastAssist, value -> config.showBedBlastAssist = value);
		y += 24;
		addToggle(x, y, "リセット確認", () -> config.confirmQuickReset, value -> config.confirmQuickReset = value);
		addToggle(x + 160, y, "プレビュー予約", () -> config.enableWorldPreview, value -> config.enableWorldPreview = value);
		y += 36;
		addRenderableWidget(Button.builder(Component.literal("保存して戻る"), button -> {
			EndrRTAConfigManager.save();
			this.minecraft.setScreen(parent);
		}).bounds(this.width / 2 - 100, y, 200, 20).build());
	}

	private void addToggle(int x, int y, String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
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
		EndrRTAConfigManager.save();
		this.minecraft.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal("競技モードでは練習補助は実行されません"), this.width / 2, 30, 0xFFAAAAAA);
	}
}
