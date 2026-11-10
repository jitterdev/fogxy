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

[Stonecutter](https://stonecutter.kikugie.dev/) is used to build the mod for multiple Minecraft versions. Each Minecraft version has a dedicated subproject.

Tasks can be run against specific versions without first having to switch to that version, as with any other Stonecutter project:

```sh
./gradlew :1.21.1:runClient      # dev client for 1.21.1
./gradlew :26.2:runClient        # dev client for 26.2
./gradlew build                  # every version; jars land in build/libs
```

To change which version is considered the "current" version (affects e.g. how code in the shared `src/` folder is presented in your IDE, etc.), run:

```sh
./gradlew stonecutterSwitchTo<version>
```

To run the currently selected version, run:

```sh
./gradlew runClient
```

Game instances are stored in `run/<version>/`. All of them will start at the title screen, and if a world is present will immediately join that world on load.

Voxy doesn't have releases for 1.21.1 on their Modrinth Maven. For those versions we manually put a Voxy jar at `libs/voxy.jar`. For all other versions the `voxy_version` in `versions/<version>/gradle.properties` is used to download the jar from their maven. In both cases `installVoxy` copies the jar into `run/<version>/mods/` before launching the client. This needs to be a jar on disk (and not on the classpath) because that's the only way Fabric Loader will extract Voxy's nested jars (RocksDB, LMDB, etc.). If you try putting Voxy on the classpath instead you'll get `NoClassDefFoundError: org/rocksdb/RocksDBException`.

- voxy has an exact dep on sodium, so those two versions need to be updated together
- Reese's Sodium Options also has to be compatible with that version of sodium
- if you update either sodium or voxy and forget to check if the other needs an update too, you won't get a build error, but a `HARD_DEP_NO_CANDIDATE` error in-game, so check the logs after updating!
- note that on 1.21.11 and 26.1.2, voxy does not support the latest version of sodium (so we use `=0.8.12` and `0.9.1` for sodium there). on those versions, we also need to downgrade Reese's Sodium Options to 2.2.3, since the 2.2.4 versions strictly dep on one specific version of sodium (`=0.8.14`, `=0.9.2`)

A jar in `libs/` takes precedence over the Modrinth release on any node, which is very useful when testing a local voxy build.

## License

Fogxy is licensed under the GNU Lesser General Public License, version 3 or later. `LICENSE` holds the LGPL-3.0 text, and `COPYING` holds GNU General Public License version 3 it incorporates.

## Disclaimer

**Fogxy is still in alpha.** Crashes and other graphical oddities/glitches should be expected. Fogxy hooks into voxy and rewrites fog related shaders as well as makes moderately invasive changes to Sodium, so don't expect it to play nice with anything else that makes changes to Sodium or voxy (hasn't been tested with any other mods as of right now). Keep a backup of your config/fogxy.json.

## Credits

Banner typeface: Alagard by Pix3M.
