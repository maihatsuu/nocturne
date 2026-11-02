# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Nocturne is a Fabric mod for Minecraft **1.21.1** (Java 21, Fabric Loader 0.20.1, Fabric API 0.116.17+1.21.1). Mod id `nocturne`, base package `maihatsuu.nocturne`.

The design spec is `../phantom-elytra-mod-plan.md`, one directory up from this repo. Read it before implementing features. In short:
- Stop Overworld phantom spawns by cancelling `PhantomSpawner.tick` with a mixin. Leave the `doInsomnia` gamerule and the insomnia stat alone.
- Spawn phantoms above End cities only, using a custom spawner on `ServerTickEvents.END_WORLD_TICK` filtered to `Level.END`, with a population cap. No insomnia check.
- Let phantom membranes be used to make elytra. The method (crafting, smithing, or something else) is still undecided, so ask before picking one.
- Add a JSON config with toggles and tuning values.
- The plan has open questions marked "Pick one". Don't settle those on your own; ask the user.
- The target is **server-side only**, unless the elytra method needs client UI.

The repo is still the unmodified Fabric example-mod template. `ExampleMixin`, `ExampleClientMixin`, the placeholder text in `fabric.mod.json` (description, authors, contact) and the "Hello Fabric world" log line are all template leftovers to replace.

## Commands

```bash
./gradlew build          # compile + remap; jar ends up in build/libs/
./gradlew runClient      # launch a dev client with the mod loaded
./gradlew runServer      # launch a dev dedicated server (good for testing the server-side spawning logic)
./gradlew genSources     # decompile Minecraft sources for browsing/searching vanilla code
```

No test suite is configured. CI (`.github/workflows/build.yml`) only runs `./gradlew build`.

## Build and layout notes

- **Mappings are official Mojang mappings** (`loom.officialMojangMappings()`), not Yarn. Use Mojang names such as `ServerLevel`, `ResourceLocation` and `PhantomSpawner`. The plan says the vanilla names it mentions still need checking against 1.21.1. Check them in the `genSources` output.
- `splitEnvironmentSourceSets()` is on. Common/server code lives in `src/main/`. Client-only code lives in `src/client/`, which can see `main` but not the other way round. Keep anything client-only (rendering, screens, `net.minecraft.client.*`) out of `src/main`.
- Mixins are registered in two configs. Server/common mixins go in `src/main/resources/nocturne.mixins.json` (package `maihatsuu.nocturne.mixin`). Client mixins go in `src/client/resources/nocturne.client.mixins.json` (package `maihatsuu.nocturne.client.mixin`). Each new mixin class has to be added to the right config's list. `injectors.defaultRequire: 1` means an injection that fails to match crashes the game at load time.
- `fabric.mod.json` currently says `"environment": "*"` and declares a client entrypoint. If the mod stays server-only, update both.
- Data-driven content (for example an elytra recipe) goes under `src/main/resources/data/nocturne/` (1.21 uses the singular folder names, e.g. `recipe/`).
- Use `Nocturne.id(path)` to build `nocturne:`-namespaced `ResourceLocation`s, and `Nocturne.LOGGER` for logging.
- Versions live in `gradle.properties`.
