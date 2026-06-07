package chihalu.endrrta.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

import chihalu.endrrta.EndrRTA;

public final class EndrRTAConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("endrrta.json");
	private static EndrRTAConfig config = new EndrRTAConfig();
	private static boolean loaded;

	private EndrRTAConfigManager() {
	}

	public static EndrRTAConfig get() {
		return config;
	}

	public static boolean isLoaded() {
		return loaded;
	}

	public static void load() {
		if (Files.notExists(CONFIG_PATH)) {
			save();
			loaded = true;
			return;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			@Nullable EndrRTAConfig loaded = GSON.fromJson(reader, EndrRTAConfig.class);
			config = loaded == null ? new EndrRTAConfig() : loaded;
			config.normalize();
		} catch (IOException exception) {
			EndrRTA.LOGGER.warn("EndraRTA 設定を読み込めませんでした。既定値を使用します。", exception);
			config = new EndrRTAConfig();
		}
		loaded = true;
	}

	public static void save() {
		try {
			config.normalize();
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			EndrRTA.LOGGER.warn("EndraRTA 設定を書き込めませんでした。", exception);
		}
	}
}
