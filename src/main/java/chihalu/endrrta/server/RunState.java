package chihalu.endrrta.server;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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

	public void stopAtDragon() {
		if (!running || stopped) {
			return;
		}

		recordSplit(SplitType.DRAGON_DEFEATED);
		stopped = true;
		running = false;
		rtaStopNanos = System.nanoTime();
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
