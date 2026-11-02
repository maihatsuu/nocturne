# Nocturne

A Fabric mod for Minecraft 1.21.1. Phantoms leave the Overworld and haunt the skies above End cities instead, and their membranes can be crafted into elytra.

Nocturne is **server-side only**. Install it on a server and vanilla clients can join. It also works in singleplayer.

## Features

### No more insomnia phantoms
Phantoms no longer spawn in the Overworld, however long you go without sleep. The `doInsomnia` gamerule and the "time since rest" statistic aren't changed; they just have no effect.

### Phantoms above End cities
When you're at or above an End city in the End, phantoms spawn in the air over the city and come straight for you.

- You only need to be close: within 32 blocks of the city's edge, and anywhere from its base up to 48 blocks above its top.
- Groups spawn 8–20 blocks above the city's top, close to you.
- Group size scales with difficulty like vanilla: 1–2 on Easy, 1–3 on Normal, 1–4 on Hard.
- Spawning pauses once there are 4 phantoms within 64 blocks of you.
- Cities never run out of phantoms.
- They despawn normally when you leave.
- Nothing spawns on Peaceful or when `doMobSpawning` is off.

### Membrane elytra
Elytra can be crafted from phantom membranes and string:

```
M S M
M . M
M . M

M = Phantom Membrane
S = String
```

The recipe unlocks in the recipe book when you pick up a phantom membrane. Elytra still appear in End ships, and membranes still repair elytra and brew Slow Falling potions.

Since membranes now only come from the End, Slow Falling and elytra repair become End-game. The "Monsters Hunted" advancement needs a phantom kill, so it can only be finished in the End.

## Configuration

The settings live in `config/nocturne.json`, which is created with the defaults on first launch. Restart the game or server after editing it.

| Option | Default | Description |
|---|---|---|
| `disableOverworldPhantoms` | `true` | Turn off vanilla insomnia phantoms. |
| `endCityPhantoms` | `true` | Spawn phantoms above End cities. |
| `spawnIntervalTicks` | `600` | Ticks between spawn attempts. A random extra of up to half this is added. |
| `spawnChance` | `0.5` | Chance of a spawn per player per attempt. |
| `heightOffsetMin` / `heightOffsetMax` | `8` / `20` | Spawn height range above the city's top. |
| `verticalRange` | `48` | How far above the city's top you still count as being at it. |
| `cityDetectionMargin` | `32` | How far outside the city's edge (horizontally) you still count as being at it. |
| `maxPhantomsPerPlayer` | `4` | Spawning stops when this many phantoms are within 64 blocks of you. |
| `scaleWithDifficulty` | `true` | Use vanilla's difficulty-based group size. |
| `groupSize` | `2` | Group size when `scaleWithDifficulty` is off. |
| `debugLogging` | `false` | Log every spawn attempt and its outcome. |

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.20.1 or later
- Fabric API
- Java 21

## Building

```bash
./gradlew build      # the jar ends up in build/libs/
./gradlew runClient  # dev client
./gradlew runServer  # dev server
```

## License

CC0-1.0. See [LICENSE](LICENSE).
