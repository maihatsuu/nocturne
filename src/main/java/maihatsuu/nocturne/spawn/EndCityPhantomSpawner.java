package maihatsuu.nocturne.spawn;

import maihatsuu.nocturne.Nocturne;
import maihatsuu.nocturne.config.NocturneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Spawns phantoms in the air above End cities, for players at or near a city.
 * Ticked from {@code ServerTickEvents.END_WORLD_TICK}; only acts in the End.
 */
public final class EndCityPhantomSpawner {
	/** Attempts to find an empty spawn position before giving up on a group. */
	private static final int POSITION_ATTEMPTS = 8;
	/**
	 * Horizontal spread of spawn positions around the player (clamped to the city footprint).
	 * Matches vanilla's insomnia spawner; it keeps phantoms inside the 16-block horizontal range
	 * their own target search uses, so they can re-acquire the player after losing them.
	 */
	private static final int SPAWN_SPREAD = 10;
	/** Radius around a player in which existing phantoms count toward that player's cap. */
	private static final double CAP_RADIUS = 64.0;

	private static int countdown;

	private EndCityPhantomSpawner() {
	}

	public static void tick(ServerLevel level) {
		if (level.dimension() != Level.END) {
			return;
		}
		NocturneConfig config = NocturneConfig.get();
		if (!config.endCityPhantoms
				|| level.getDifficulty() == Difficulty.PEACEFUL
				|| !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
			return;
		}

		RandomSource random = level.random;
		if (--countdown > 0) {
			return;
		}
		countdown = config.spawnIntervalTicks + random.nextInt(config.spawnIntervalTicks / 2 + 1);

		Structure endCity = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.END_CITY);
		if (endCity == null) {
			return;
		}

		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			BoundingBox city = findCityNear(level, endCity, player.blockPosition(), config);
			if (city == null) {
				debug(config, "{}: not near an End city", player.getName().getString());
				continue;
			}
			if (random.nextDouble() >= config.spawnChance) {
				debug(config, "{}: near city, spawn roll failed", player.getName().getString());
				continue;
			}
			spawnGroup(level, player, city, random, config);
		}
	}

	/**
	 * Finds an End city whose footprint, widened by {@code cityDetectionMargin}, contains the
	 * player horizontally, with the player between the city's bottom and {@code verticalRange}
	 * above its top. Checks the chunks around the player because structure references only
	 * exist in chunks the city actually overlaps.
	 */
	private static BoundingBox findCityNear(ServerLevel level, Structure endCity, BlockPos pos, NocturneConfig config) {
		int margin = config.cityDetectionMargin;
		int chunkRadius = Mth.ceil(margin / 16.0);
		ChunkPos center = new ChunkPos(pos);
		for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
			for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
				int cx = center.x + dx;
				int cz = center.z + dz;
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(cx, cz), s -> s == endCity)) {
					BoundingBox box = start.getBoundingBox();
					if (pos.getX() >= box.minX() - margin && pos.getX() <= box.maxX() + margin
							&& pos.getZ() >= box.minZ() - margin && pos.getZ() <= box.maxZ() + margin
							&& pos.getY() >= box.minY() - margin && pos.getY() <= box.maxY() + config.verticalRange) {
						return box;
					}
				}
			}
		}
		return null;
	}

	private static void spawnGroup(ServerLevel level, ServerPlayer player, BoundingBox city, RandomSource random, NocturneConfig config) {
		String name = player.getName().getString();
		int existing = level.getEntitiesOfClass(Phantom.class, player.getBoundingBox().inflate(CAP_RADIUS)).size();
		int room = config.maxPhantomsPerPlayer - existing;
		if (room <= 0) {
			debug(config, "{}: cap reached ({} phantoms nearby)", name, existing);
			return;
		}

		BlockPos pos = findSpawnPos(level, player.blockPosition(), city, random, config);
		if (pos == null) {
			debug(config, "{}: no empty spawn position found above city", name);
			return;
		}

		DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
		int count = config.scaleWithDifficulty
				? 1 + random.nextInt(difficulty.getDifficulty().getId() + 1)
				: config.groupSize;
		count = Math.min(count, room);

		SpawnGroupData groupData = null;
		for (int i = 0; i < count; i++) {
			Phantom phantom = EntityType.PHANTOM.create(level);
			if (phantom == null) {
				continue;
			}
			phantom.moveTo(pos, 0.0F, 0.0F);
			groupData = phantom.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, groupData);
			// Aim the group at the player they spawned for straight away, instead of waiting for
			// the phantom's own scan to find them. It still needs line of sight to keep the target.
			phantom.setTarget(player);
			level.addFreshEntityWithPassengers(phantom);
		}
		debug(config, "{}: spawned {} phantom(s) at {} ({} already nearby)", name, count, pos.toShortString(), existing);
	}

	/** Picks a spot above the city's top, near the player but inside the city's footprint. */
	private static BlockPos findSpawnPos(ServerLevel level, BlockPos playerPos, BoundingBox city, RandomSource random, NocturneConfig config) {
		for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
			int x = Mth.clamp(playerPos.getX() + random.nextIntBetweenInclusive(-SPAWN_SPREAD, SPAWN_SPREAD), city.minX(), city.maxX());
			int z = Mth.clamp(playerPos.getZ() + random.nextIntBetweenInclusive(-SPAWN_SPREAD, SPAWN_SPREAD), city.minZ(), city.maxZ());
			int y = city.maxY() + random.nextIntBetweenInclusive(config.heightOffsetMin, config.heightOffsetMax);
			BlockPos pos = new BlockPos(x, Math.min(y, level.getMaxBuildHeight() - 2), z);
			if (level.isLoaded(pos) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
				return pos;
			}
		}
		return null;
	}

	private static void debug(NocturneConfig config, String message, Object... args) {
		if (config.debugLogging) {
			Nocturne.LOGGER.info("[spawn] " + message, args);
		}
	}
}
