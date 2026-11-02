package maihatsuu.nocturne.spawn;

import maihatsuu.nocturne.config.NocturneConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * Spawns phantoms in the air above End cities, for players at or above a city.
 * Ticked from {@code ServerTickEvents.END_WORLD_TICK}; only acts in the End.
 */
public final class EndCityPhantomSpawner {
	/** Attempts to find an empty spawn position before giving up on a group. */
	private static final int POSITION_ATTEMPTS = 8;
	/** Extra headroom above the highest spawn height when counting phantoms for the cap. */
	private static final int CAP_MARGIN = 16;

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

		// A city visited by several players is only considered once per attempt.
		Set<ChunkPos> handledCities = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			StructureStart city = findCityAt(level, endCity, player.blockPosition(), config);
			if (city == null || !handledCities.add(city.getChunkPos())) {
				continue;
			}
			if (random.nextDouble() < config.spawnChance) {
				spawnGroup(level, city.getBoundingBox(), random, config);
			}
		}
	}

	private static StructureStart findCityAt(ServerLevel level, Structure endCity, BlockPos pos, NocturneConfig config) {
		for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(pos), s -> s == endCity)) {
			BoundingBox box = start.getBoundingBox();
			if (pos.getX() >= box.minX() && pos.getX() <= box.maxX()
					&& pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ()
					&& pos.getY() >= box.minY() && pos.getY() <= box.maxY() + config.verticalRange) {
				return start;
			}
		}
		return null;
	}

	private static void spawnGroup(ServerLevel level, BoundingBox box, RandomSource random, NocturneConfig config) {
		AABB capArea = new AABB(
				box.minX(), box.minY(), box.minZ(),
				box.maxX() + 1, box.maxY() + config.heightOffsetMax + CAP_MARGIN, box.maxZ() + 1);
		int existing = level.getEntitiesOfClass(Phantom.class, capArea).size();
		int room = config.maxPhantomsPerCity - existing;
		if (room <= 0) {
			return;
		}

		BlockPos pos = findSpawnPos(level, box, random, config);
		if (pos == null) {
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
			level.addFreshEntityWithPassengers(phantom);
		}
	}

	private static BlockPos findSpawnPos(ServerLevel level, BoundingBox box, RandomSource random, NocturneConfig config) {
		for (int attempt = 0; attempt < POSITION_ATTEMPTS; attempt++) {
			int x = box.minX() + random.nextInt(box.getXSpan());
			int z = box.minZ() + random.nextInt(box.getZSpan());
			int y = box.maxY() + config.heightOffsetMin
					+ random.nextInt(config.heightOffsetMax - config.heightOffsetMin + 1);
			BlockPos pos = new BlockPos(x, Math.min(y, level.getMaxBuildHeight() - 2), z);
			if (level.isLoaded(pos) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
				return pos;
			}
		}
		return null;
	}
}
