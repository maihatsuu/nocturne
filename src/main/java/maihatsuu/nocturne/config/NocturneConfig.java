package maihatsuu.nocturne.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import maihatsuu.nocturne.Nocturne;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class NocturneConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(Nocturne.MOD_ID + ".json");
	private static NocturneConfig instance = new NocturneConfig();

	/** Stop the vanilla insomnia spawner from spawning phantoms. */
	public boolean disableOverworldPhantoms = true;
	/** Spawn phantoms above End cities. */
	public boolean endCityPhantoms = true;
	/** Ticks between spawn attempts (a random extra of up to half this is added). */
	public int spawnIntervalTicks = 600;
	/** Chance per eligible player per spawn attempt. */
	public double spawnChance = 0.5;
	/** Spawn height range above the city's top. */
	public int heightOffsetMin = 8;
	public int heightOffsetMax = 20;
	/** How far above the city's top a player still counts as "at" the city. */
	public int verticalRange = 48;
	/** Maximum phantoms in the air above a single city. */
	public int maxPhantomsPerCity = 6;
	/** Group size is 1 + random(0..difficulty) like vanilla; otherwise groupSize. */
	public boolean scaleWithDifficulty = true;
	public int groupSize = 2;

	public static NocturneConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				NocturneConfig loaded = GSON.fromJson(reader, NocturneConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonParseException e) {
				Nocturne.LOGGER.warn("Failed to read {}, using defaults", PATH, e);
				return;
			}
		}
		instance.sanitize();
		save();
	}

	private static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			Nocturne.LOGGER.warn("Failed to write {}", PATH, e);
		}
	}

	private void sanitize() {
		spawnIntervalTicks = Math.max(1, spawnIntervalTicks);
		spawnChance = Math.clamp(spawnChance, 0.0, 1.0);
		heightOffsetMin = Math.max(0, heightOffsetMin);
		heightOffsetMax = Math.max(heightOffsetMin, heightOffsetMax);
		verticalRange = Math.max(0, verticalRange);
		maxPhantomsPerCity = Math.max(0, maxPhantomsPerCity);
		groupSize = Math.max(1, groupSize);
	}
}
