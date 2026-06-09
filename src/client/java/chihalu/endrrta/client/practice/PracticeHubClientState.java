package chihalu.endrrta.client.practice;

public final class PracticeHubClientState {
	private static boolean active;

	private PracticeHubClientState() {
	}

	public static void markActive() {
		active = true;
	}

	public static boolean isActive() {
		return active;
	}
}
