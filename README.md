![Fogxy banner](assets/banner.png?v=eb25546)

Fogxy is a mod that aims to provide world-wrapped fog for the voxy LOD mod. Instead of cylindrical fog around the camera, fog will hug the boundary of the world and cover any holes.

## Compatibility

Fogxy supports Fabric across Minecraft 1.21.1, 1.21.6-1.21.8, 1.21.9-1.21.10, 1.21.11, 26.1-26.1.2, and 26.2, covering voxy v0.2.4-alpha through v0.2.19-beta and Sodium v0.6 through v0.9.

| Minecraft | Loader | Voxy | Sodium |
| --- | --- | --- | --- |
| 1.21.1 | Fabric | v0.2.14-alpha | 0.6.13 or newer |
| 1.21.1 | NeoForge via Sinytra Connector and Roxy | v0.2.16-beta | 0.8.13 |
| 1.21.6 - 1.21.8 | Fabric | v0.2.5-alpha | 0.7.x |
| 1.21.9 - 1.21.10 | Fabric | v0.2.9-alpha | 0.7.3 |
| 1.21.11 | Fabric | v0.2.16-beta | 0.8.4, 0.8.6, 0.8.7, 0.8.11, 0.8.12 |
| 26.1 - 26.1.2 | Fabric | v0.2.18-beta | 0.9.1 |
| 26.2 | Fabric | v0.2.19-beta | 0.9.1 or 0.9.2 |

All jars (when ran on NeoForge) have a required dependency on Sinytra Connector. There is no separate NeoForge build, because voxy itself does not ship on NeoForge; however, you should be able to load all Fabric builds on NeoForge with Sinytra Connector.

**You have permission to report issues that arise from using Sinytra Connector with Fogxy to this repository**.

## Configuration

`config/fogxy.json`, also editable from the **Fog** tab in Sodium's video settings.

| Option          | Default | Meaning                                                                                                                                                                                            |
|-----------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `enabled`       | `true`  | Constrain fog to the terrain edge.                                                                                                                                                                 |
| `fog_start`     | `0.5`   | Where the fog starts fading in on the way to the terrain border, as a percentage.                                                                                                                  |
| `fog_end`       | `1.0`   | Where the fog reaches full. `1.0` is the border itself.                                                                                                                                            |
| `smooth`        | `16`    | Rays the per-direction distance is averaged over.                                                                                                                                                  |
| `slope`         | `64`    | Most the fog end may move between neighbouring directions, in blocks.                                                                                                                              |
| `holeReach`     | `64`    | How far the fog block reaches past a coarse patch, in blocks.                                                                                                                                      |
| `holeFade`      | `128`   | How far the fog block fades out past the reach, in blocks.                                                                                                                                         |
| `levels`        | `2`     | How many of voxy's finest LOD levels count as detailed terrain.                                                                                                                                    |
| `holeFill`      | `true`  | Paint solid fog across inner holes where voxy draws no geometry at all, instead of leaving a see-through patch of sky.                                                                             |
| `fillTop`       | `96`    | World height the hole fill mass reaches. Set it near the height of the terrain around the holes; higher fills more sky above a hole seen across it, lower can leave a sliver at the top of a hole. |
| `skyFog`        | `false` | Color the fog in each direction from the sky's sunrise and sunset gradient, instead of the single camera-based color vanilla picks, so wide views blend into the sky at dawn and dusk.             |
| `debugGradient` | `false` |                                                                                                                                                                                                    |
| `debugEdges`    | `false` |                                                                                                                                                                                                    |

## Development

The project builds for several versions using [Stonecutter](https://stonecutter.kikugie.dev/). Each Minecraft version is its own Gradle subproject. You can run any task for a specific version directly, without switching anything first, like a standard Stonecutter project:

```sh
./gradlew :1.21.1:runClient      # dev client for 1.21.1
./gradlew :26.2:runClient        # dev client for 26.2
./gradlew build                  # every version; jars land in build/libs
```

The `stonecutterSwitchTo<version>` change which version is marked as active in `stonecutter.gradle.kts`, which decides how the shared `src/` code in your working files and in your IDE. To run whichever version is currently active, use:

```sh
./gradlew runClient
```

Runs are isolated by version in `run/<version>/`. This means a newer client will never upgrade an older world or overwrite its `options.txt`. All versions open on the main menu until you create a world, in which they will quick-join into it on load.

Voxy is pulled at runtime from the Modrinth Maven per version by `voxy_version` in `versions/<version>/gradle.properties`, and staged into `run/<version>/mods/` by the `installVoxy` task before the client starts. It has to be a jar on disk rather than a classpath dependency. Fabric Loader only unwraps a mod's nested jar-in-jars it finds as files. Voxy bundles RocksDB, LMDB and friends that way. This means loading voxy as a classpath causes a `NoClassDefFoundError: org/rocksdb/RocksDBException`.

Note that **1.21.1 has no voxy release on the Modrinth Maven**, so for that node you drop your own jar at `libs/voxy.jar` and `installVoxy` stages it the same way. Also, a jar in `libs/` takes precedence over the Modrinth release on any node, which is very useful when testing a local voxy build.

Voxy requires one exact Sodium version, so `voxy_version` and `sodium_version` have to be changed together. `reeses_sodium_options_version` also has to be a build that accepts whichever Sodium you pinned.

The 2.2.4 builds of Reese's Sodium Options lock Sodium to a single version (`=0.8.14`, `=0.9.2`). On 1.21.11 and 26.1.2, voxy rejects the newest Sodium (`=0.8.12` and `0.9.1` respectively). These nodes use the 2.2.3 builds instead.

A mismatch will not cause a compile error, but a `HARD_DEP_NO_CANDIDATE` in the run log. Check the log whenever you bump either version.

## License

Fogxy is licensed under the GNU Lesser General Public License, version 3 or later. `LICENSE` holds the LGPL-3.0 text, and `COPYING` holds GNU General Public License version 3 it incorporates.

## Disclaimer

**Fogxy is alpha software.** It hooks into voxy's renderer, rewrites the fog shaders, and does moderately invasive changes to Sodium. Instability is likely, and to be expected. It has not yet been tested with other mods, and anything else that touches voxy or Sodium's rendering may conflict with it. Keep a backup of config/fogxy.json. Crashes and visual glitches or oddities are both on the table.

## Credits

Banner typeface: Alagard by Pix3M.
