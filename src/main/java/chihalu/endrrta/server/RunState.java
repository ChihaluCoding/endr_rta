package chihalu.endrrta.server;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class RunState {
	private boolean running;
	private boolean stopped;
	private long rtaStartNanos;
	private long rtaStopNanos;
	private long igtTicks;
	private @Nullable ResourceKey<Level> lastDimension;
	private @Nullable RadarTarget stronghold;
	private @Nullable RadarTarget fortress;
	private String bastionType = "未検出";
	private boolean bastionFound = false;
	private final EnumSet<@NonNull SplitType> recordedSplits = EnumSet.noneOf(SplitType.class);
	private final List<@NonNull SplitRecord> splits = new ArrayList<>();

	public void startIfNeeded(ResourceKey<Level> dimension) {
		if (running || stopped) {
			return;
		}

		running = true;
		rtaStartNanos = System.nanoTime();
		lastDimension = dimension;
	}

	public void tickIgt() {
		if (running && !stopped) {
			igtTicks++;
		}
	}

	public boolean stopAtDragon() {
		if (!running || stopped) {
			return false;
		}

		recordSplit(SplitType.DRAGON_DEFEATED);
		stopped = true;
		running = false;
		rtaStopNanos = System.nanoTime();
		return true;
	}

	public void recordSplit(@NonNull SplitType type) {
		if (!running || stopped || recordedSplits.contains(type)) {
			return;
		}

		recordedSplits.add(type);
		splits.add(new SplitRecord(type, elapsedRtaMillis(), igtTicks));
	}

	public boolean hasRecordedSplit(@NonNull SplitType type) {
		return recordedSplits.contains(type);
	}

	public long elapsedRtaMillis() {
		if (!running && !stopped) {
			return 0L;
		}

		long end = stopped ? rtaStopNanos : System.nanoTime();
		return Math.max(0L, (end - rtaStartNanos) / 1_000_000L);
	}

	public long igtTicks() {
		return igtTicks;
	}

	public @Nullable ResourceKey<Level> lastDimension() {
		return lastDimension;
	}

	public void setLastDimension(ResourceKey<Level> lastDimension) {
		this.lastDimension = lastDimension;
	}

	public @Nullable RadarTarget stronghold() {
		return stronghold;
	}

	public void setStronghold(@Nullable RadarTarget stronghold) {
		this.stronghold = stronghold;
	}

	public @Nullable RadarTarget fortress() {
		return fortress;
	}

	public void setFortress(@Nullable RadarTarget fortress) {
		this.fortress = fortress;
	}

	public String bastionType() {
		return bastionType;
	}

	public void setBastionType(String bastionType) {
		this.bastionType = bastionType;
	}

	public boolean bastionFound() {
		return bastionFound;
	}

	public void setBastionFound(boolean bastionFound) {
		this.bastionFound = bastionFound;
	}

	public RunStateSnapshot snapshot() {
		RunStateSnapshot snapshot = new RunStateSnapshot();
		snapshot.running = running;
		snapshot.stopped = stopped;
		snapshot.elapsedRtaMillis = elapsedRtaMillis();
		snapshot.igtTicks = igtTicks;
		snapshot.lastDimension = lastDimension == null ? "" : lastDimension.identifier().toString();
		for (SplitRecord split : splits) {
			RunStateSnapshot.SplitSnapshot splitSnapshot = new RunStateSnapshot.SplitSnapshot();
			splitSnapshot.type = split.type().name();
			splitSnapshot.rtaMillis = split.rtaMillis();
			splitSnapshot.igtTicks = split.igtTicks();
			snapshot.splits.add(splitSnapshot);
		}
		return snapshot;
	}

	public static RunState restore(RunStateSnapshot snapshot) {
		RunState run = new RunState();
		run.running = snapshot.running && !snapshot.stopped;
		run.stopped = snapshot.stopped;
		run.igtTicks = Math.max(0L, snapshot.igtTicks);
		run.lastDimension = parseDimension(snapshot.lastDimension);

		long now = System.nanoTime();
		long elapsedNanos = Math.max(0L, snapshot.elapsedRtaMillis) * 1_000_000L;
		if (run.running || run.stopped) {
			run.rtaStartNanos = now - elapsedNanos;
			run.rtaStopNanos = run.stopped ? now : 0L;
		}

		for (RunStateSnapshot.SplitSnapshot splitSnapshot : snapshot.splits) {
			try {
				SplitType type = SplitType.valueOf(splitSnapshot.type);
				run.recordedSplits.add(type);
				run.splits.add(new SplitRecord(type, splitSnapshot.rtaMillis, splitSnapshot.igtTicks));
			} catch (IllegalArgumentException ignored) {
				// 古い保存データに未知のスプリットが含まれていても、読み込み全体は継続する。
			}
		}
		if (run.recordedSplits.contains(SplitType.DRAGON_DEFEATED)) {
			run.running = false;
			run.stopped = true;
			if (run.rtaStartNanos == 0L) {
				run.rtaStartNanos = now - elapsedNanos;
			}
			run.rtaStopNanos = run.rtaStartNanos + elapsedNanos;
		}
		return run;
	}

	private static @Nullable ResourceKey<Level> parseDimension(String dimensionId) {
		if (dimensionId == null || dimensionId.isBlank()) {
			return null;
		}
		try {
			return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimensionId));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	public List<@NonNull SplitRecord> splits() {
		return List.copyOf(splits);
	}

	public @Nullable SplitRecord latestSplit() {
		return splits.isEmpty() ? null : splits.getLast();
	}

	public boolean stopped() {
		return stopped;
	}
}
