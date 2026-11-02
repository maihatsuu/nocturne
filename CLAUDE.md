# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Nocturne is a Fabric mod for Minecraft **1.21.1** (Java 21, Fabric Loader 0.20.1, Fabric API 0.116.17+1.21.1). Mod id `nocturne`, base package `maihatsuu.nocturne`.

The design spec is `../phantom-elytra-mod-plan.md`, one directory up from this repo. Its open questions have been settled:
- **Elytra:** a shaped crafting recipe: 7 phantom membranes around a popped chorus fruit.
- **End-city phantoms:** vanilla despawning; cities spawn forever, up to a per-city cap; group size scales with difficulty like vanilla.
- **No client code.** The mod is server-side only: vanilla clients can join, because the recipe syncs from the server. It still installs on the client for singleplayer, so `"environment": "*"` stays.

How the pieces fit:
- `mixin/PhantomSpawnerMixin`: cancels `PhantomSpawner.tick` at HEAD, which turns off Overworld insomnia phantoms. The `doInsomnia` gamerule and the `TIME_SINCE_REST` stat are deliberately left alone.
- `spawn/EndCityPhantomSpawner`: ticked from `ServerTickEvents.END_WORLD_TICK` (registered in `Nocturne.onInitialize`), and only acts in `Level.END`.
  - It finds the city around a player with `StructureManager.startsForStructure(ChunkPos, ...)` plus a check against the city's bounding-box footprint. `getStructureWithPieceAt` would miss players flying above the pieces.
  - It spawns the group above the box's max Y, using the same create / `finalizeSpawn(NATURAL)` / `addFreshEntityWithPassengers` sequence as vanilla.
- `config/NocturneConfig`: a Gson POJO at `config/nocturne.json`, loaded in `onInitialize` and read via `NocturneConfig.get()`. On load, missing fields fall back to defaults and the file is rewritten.
- Data: `data/nocturne/recipe/elytra.json`, plus a recipe-book unlock advancement under `data/nocturne/advancement/recipes/`.

## Commands

```bash
./gradlew build          # compile + remap; jar ends up in build/libs/
./gradlew runClient      # launch a dev client with the mod loaded
./gradlew runServer      # launch a dev dedicated server (good for testing the server-side spawning logic)
./gradlew genSources     # decompile Minecraft sources for browsing/searching vanilla code
```

No test suite is configured. CI (`.github/workflows/build.yml`) only runs `./gradlew build`.

## Build and layout notes

- **Mappings are official Mojang mappings** (`loom.officialMojangMappings()`), not Yarn. Use Mojang names such as `ServerLevel`, `ResourceLocation` and `PhantomSpawner`. Check vanilla names and signatures in the `genSources` output.
- There is a single `main` source set (the template's client source set was removed). Don't reference `net.minecraft.client.*`.
- Mixins are listed in `src/main/resources/nocturne.mixins.json` (package `maihatsuu.nocturne.mixin`). `injectors.defaultRequire: 1` means an injection that fails to match crashes the game at load time.
- 1.21.1 still uses `MobSpawnType` (it was renamed `EntitySpawnReason` in later versions). Data folders use the singular names (`recipe/`, `advancement/`), and recipe results use `"id"`.
- Use `Nocturne.id(path)` to build `nocturne:`-namespaced `ResourceLocation`s, and `Nocturne.LOGGER` for logging.
- Versions live in `gradle.properties`.
