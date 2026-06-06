package chihalu.endrrta.server;

public enum SplitType {
	NETHER_ENTER("ネザー入場"),
	NETHER_FORTRESS_FOUND("ネザー要塞発見"),
	STRONGHOLD_FOUND("エンド要塞発見"),
	END_ENTER("エンド入場"),
	DRAGON_DEFEATED("ドラゴン撃破");

	private final String label;

	SplitType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
