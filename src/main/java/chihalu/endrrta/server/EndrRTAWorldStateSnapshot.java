package chihalu.endrrta.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EndrRTAWorldStateSnapshot {
	public final Map<String, RunStateSnapshot> runs = new HashMap<>();
	public final Map<String, PracticeStartPointSnapshot> practiceStartPoints = new HashMap<>();
	public final List<String> villageSpawnedPlayers = new ArrayList<>();
	public final List<String> practiceStartChestPlayers = new ArrayList<>();
}
