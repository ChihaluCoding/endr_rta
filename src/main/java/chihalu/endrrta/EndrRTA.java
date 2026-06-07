package chihalu.endrrta;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import chihalu.endrrta.config.EndrRTAConfigManager;
import chihalu.endrrta.server.EntityDropTransformer;
import chihalu.endrrta.server.EndrRTAServerState;
import chihalu.endrrta.server.HayBaleClusterBreaker;
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
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> EndrRTAServerState.prepareInitialSpawn(handler.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(EndrRTAServerState::tickServer);
		ServerLifecycleEvents.SERVER_STARTED.register(EndrRTAServerState::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(EndrRTAServerState::save);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> EndrRTAServerState.clear());
		PlayerBlockBreakEvents.AFTER.register(HayBaleClusterBreaker::breakNearbyHayBales);
		LootTableEvents.MODIFY_DROPS.register(OreDropTransformer::replaceRawOreDrops);
		LootTableEvents.MODIFY_DROPS.register(EntityDropTransformer::ensurePracticeDrops);
		LOGGER.info("EnderRTA を初期化しました。");
	}
}
