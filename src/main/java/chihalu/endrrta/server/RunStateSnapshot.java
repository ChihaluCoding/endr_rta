package chihalu.endrrta.server;

import java.util.ArrayList;
import java.util.List;

public final class RunStateSnapshot {
	public boolean running;
	public boolean stopped;
	public long elapsedRtaMillis;
	public long igtTicks;
	public String lastDimension = "";
	public final List<SplitSnapshot> splits = new ArrayList<>();

	public static final class SplitSnapshot {
		public String type = "";
		public long rtaMillis;
		public long igtTicks;
	}
}
