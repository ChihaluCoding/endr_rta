package chihalu.endrrta.config;

public class EndrRTAConfig {
	public boolean practiceMode = true;
	public boolean fullBright = false;
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
	public boolean showFortressWarpedDistance = true;
	public boolean generateFortressWarpedPortal = false;
	public boolean breakNearbyHayBales = true;
	public boolean placePracticeStartChest = false;
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
	public boolean guideEndermenToBoats = true;
	public boolean confirmQuickReset = true;
	public boolean resetKeepPreviousWorld = true;
	public boolean enableMultiInstance = false;
	public boolean resetGenerateStructures = true;
	public boolean resetBonusChest = false;
	public boolean resetAllowCommands = false;
	public boolean resetHardcore = false;
	public String practiceHubWorldId = "RTAWorld";
	public String practiceHubTemplatePath = "I:\\Programming\\PF\\Minecraft Mod\\endrrta-26.1.2\\RTAWorld";
	public String practiceScenario = "none";
	public int villageSearchRadius = 128;
	public int radarSearchRadius = 256;
	public int structureFoundDistance = 96;

	public boolean isCompetitionMode() {
		return !practiceMode;
	}

	public boolean allowsPracticeAssist() {
		return practiceMode;
	}

	public boolean allowsForceVillageSpawn() {
		return allowsPracticeAssist() && forceVillageSpawn;
	}

	public boolean allowsFullBright() {
		return allowsPracticeAssist() && fullBright;
	}

	public boolean allowsRadar() {
		return allowsPracticeAssist() && showRadar;
	}

	public boolean allowsShowFortressWarpedDistance() {
		return allowsPracticeAssist() && showFortressWarpedDistance;
	}

	public boolean allowsGenerateFortressWarpedPortal() {
		return allowsPracticeAssist() && generateFortressWarpedPortal;
	}

	public boolean allowsBreakNearbyHayBales() {
		return allowsPracticeAssist() && breakNearbyHayBales;
	}

	public boolean allowsPlacePracticeStartChest() {
		return allowsPracticeAssist() && placePracticeStartChest;
	}

	public boolean allowsAutoSmeltOres() {
		return allowsPracticeAssist() && autoSmeltOres;
	}

	public boolean allowsUnbreakableEnderEyes() {
		return allowsPracticeAssist() && unbreakableEnderEyes;
	}

	public boolean allowsPreventIgnoredItemPickup() {
		return allowsPracticeAssist() && preventIgnoredItemPickup;
	}

	public boolean allowsGuaranteedBlazeRods() {
		return allowsPracticeAssist() && guaranteedBlazeRods;
	}

	public boolean allowsGuaranteedEnderPearls() {
		return allowsPracticeAssist() && guaranteedEnderPearls;
	}

	public boolean allowsPreventEndermanBlockCarry() {
		return allowsPracticeAssist() && preventEndermanBlockCarry;
	}

	public boolean allowsGuideEndermenToBoats() {
		return allowsPracticeAssist() && guideEndermenToBoats;
	}

	public boolean allowsResetBonusChest() {
		return allowsPracticeAssist() && resetBonusChest;
	}

	public boolean allowsResetCommands() {
		return allowsPracticeAssist() && resetAllowCommands;
	}

	public boolean allowsPracticeScenario() {
		return allowsPracticeAssist() && !"none".equals(practiceScenario);
	}

	public void normalize() {
		hudBackgroundOpacity = Math.clamp(hudBackgroundOpacity, 0, 100);
		if (ignoredPickupItems == null) {
			ignoredPickupItems = new String[0];
		}
		if (practiceHubWorldId == null || practiceHubWorldId.isBlank() || "EnderRTA-Practice".equals(practiceHubWorldId)) {
			practiceHubWorldId = "RTAWorld";
		}
		if (practiceHubTemplatePath == null || practiceHubTemplatePath.isBlank()) {
			practiceHubTemplatePath = "I:\\Programming\\PF\\Minecraft Mod\\endrrta-26.1.2\\RTAWorld";
		}
		practiceScenario = normalizePracticeScenario(practiceScenario);
	}

	private static String normalizePracticeScenario(String value) {
		return switch (value == null ? "" : value) {
			case "nether_fortress", "bastion", "warped_forest_pearls", "lava_pool_portal", "ender_dragon" -> value;
			default -> "none";
		};
	}
}
