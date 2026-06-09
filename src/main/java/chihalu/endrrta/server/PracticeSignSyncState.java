package chihalu.endrrta.server;

public final class PracticeSignSyncState {
	private static volatile boolean pendingSign;

	private PracticeSignSyncState() {
	}

	public static boolean hasPendingSign() {
		return pendingSign;
	}

	public static void setPendingSign(boolean pending) {
		pendingSign = pending;
	}
}
