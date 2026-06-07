package chihalu.endrrta.config;

public class EndrRTAConfig {
	public boolean practiceMode = true;
	public boolean forceVillageSpawn = true;
	public boolean autoSmeltOres = true;
	public boolean unbreakableEnderEyes = true;
	public boolean autoSplits = true;
	public boolean showHud = true;
	public boolean showCoordinateConversion = true;
	public boolean showBiome = true;
	public boolean showLightLevel = true;
	public boolean showCrystalCount = true;
	public boolean showBastionType = true;
	public int hudBackgroundOpacity = 90;
	public boolean showRadar = true;
	public boolean showBedBlastAssist = true;
	public boolean breakNearbyHayBales = true;
	public boolean preventIgnoredItemPickup = false;
	public String[] ignoredPickupItems = {
			"minecraft:netherrack",
			"minecraft:basalt",
			"minecraft:blackstone"
	};
	public boolean enablePieChartAssist = true;
	public boolean guaranteedBlazeRods = true;
	public boolean guaranteedEnderPearls = true;
	public boolean preventEndermanBlockCarry = true;
	public boolean confirmQuickReset = true;
	public boolean enableMultiInstance = false;
	public boolean resetGenerateStructures = true;
	public boolean resetBonusChest = false;
	public boolean resetAllowCommands = false;
	public boolean resetHardcore = false;
	public int villageSearchRadius = 128;
	public int radarSearchRadius = 256;
	public int structureFoundDistance = 96;

	public boolean isCompetitionMode() {
		return !practiceMode;
	}

	public boolean allowsPracticeAssist() {
		return practiceMode;
	}

	public void normalize() {
		hudBackgroundOpacity = Math.clamp(hudBackgroundOpacity, 0, 100);
		if (ignoredPickupItems == null) {
			ignoredPickupItems = new String[0];
		}
	}
}
