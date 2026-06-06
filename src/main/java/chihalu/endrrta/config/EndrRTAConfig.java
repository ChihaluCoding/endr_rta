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
	public boolean showRadar = true;
	public boolean showBedBlastAssist = true;
	public boolean confirmQuickReset = true;
	public boolean enableWorldPreview = false;
	public boolean enableMultiInstance = false;
	public int villageSearchRadius = 128;
	public int radarSearchRadius = 256;
	public int structureFoundDistance = 96;

	public boolean isCompetitionMode() {
		return !practiceMode;
	}

	public boolean allowsPracticeAssist() {
		return practiceMode;
	}
}
