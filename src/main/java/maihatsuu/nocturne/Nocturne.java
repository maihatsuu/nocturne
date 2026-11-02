package maihatsuu.nocturne;

import maihatsuu.nocturne.config.NocturneConfig;
import maihatsuu.nocturne.spawn.EndCityPhantomSpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nocturne implements ModInitializer {
	public static final String MOD_ID = "nocturne";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		NocturneConfig.load();
		ServerTickEvents.END_WORLD_TICK.register(EndCityPhantomSpawner::tick);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
