package chihalu.endrrta;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import chihalu.endrrta.config.EndrRTAConfigManager;
import chihalu.endrrta.server.EndrRTAServerState;
import chihalu.endrrta.server.OreDropTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndrRTA implements ModInitializer {
	public static final String MOD_ID = "endrrta";

	// ログ出力元が分かるように、mod ID をロガー名として使う。
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		EndrRTAConfigManager.load();
		ServerTickEvents.END_SERVER_TICK.register(EndrRTAServerState::tickServer);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> EndrRTAServerState.clear());
		LootTableEvents.MODIFY_DROPS.register(OreDropTransformer::replaceRawOreDrops);
		LOGGER.info("EndrRTA を初期化しました。");
	}
}
