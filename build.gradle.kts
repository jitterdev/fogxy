plugins {
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

fun prop(name: String): String = property(name) as String

version = "${prop("mod_version")}+${sc.current.version}"
group = prop("maven_group")

base {
    archivesName = prop("archives_base_name")
}

repositories {
    maven("https://maven.fabricmc.net/") {
        name = "Fabric"
    }
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.terraformersmc.com/releases/") {
        name = "TerraformersMC"
    }
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
        name = "CentralMirror"
    }
}

val voxyStubs: SourceSet = sourceSets.create("voxyStubs")

// Stand-ins for Sodium 0.8+ config types so the fallback mixin compiles against older Sodium jars.
if (sc.current.parsed < "1.21.11") {
    sourceSets.main.get().java.srcDir(rootProject.file("src/sodiumStubs/java"))
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

    compileOnly(voxyStubs.output)

    modCompileOnly("maven.modrinth:sodium:${prop("sodium_version")}")
    modRuntimeOnly("maven.modrinth:sodium:${prop("sodium_version")}")

    modRuntimeOnly("maven.modrinth:modmenu:${prop("modmenu_version")}")
    modRuntimeOnly("maven.modrinth:reeses-sodium-options:${prop("reeses_sodium_options_version")}")
}

// One run directory per node; a shared one lets a newer version upgrade the world and options.
val clientRunDir = rootProject.layout.projectDirectory.dir("run/${sc.current.version}")
val runModsDir = clientRunDir.dir("mods")

// Voxy bundles its storage libraries as nested jars (RocksDB, LMDB, LZ4), and Loader only unwraps
// those for mods it finds as files. On the classpath they are invisible, so Voxy dies with
// NoClassDefFoundError on org.rocksdb.RocksDBException. Stage the jar in the run's mods folder
// instead. A jar in libs/ wins, which is how 1.21.1 works since it has no Modrinth release.
val voxyModJar = rootProject.file("libs/voxy-${sc.current.version}.jar")
val voxyModLibs = rootProject.file("libs/voxy.jar")
val voxyVersionProp = findProperty("voxy_version") as String?

val voxyModSource: Any? = when {
    voxyModJar.exists() -> files(voxyModJar)
    voxyModLibs.exists() -> files(voxyModLibs)
    voxyVersionProp != null -> configurations.detachedConfiguration(
        dependencies.create("maven.modrinth:voxy:$voxyVersionProp")
    ).apply { isTransitive = false }
    else -> null
}

val voxyInRunMods: Boolean = runModsDir.asFile.listFiles()
    ?.any { it.isFile && it.name.startsWith("voxy") && it.name.endsWith(".jar") } == true

if (voxyModSource != null) {
    val installVoxy = tasks.register<Copy>("installVoxy") {
        group = "fabric"
        description = "Stages Voxy in the run directory so its nested jars load"
        from(voxyModSource)
        into(runModsDir)
        rename { "voxy.jar" }

        // Loader rejects duplicate mod ids, so only one Voxy copy may sit in mods/.
        doFirst {
            runModsDir.asFile.listFiles()
                ?.filter { it.isFile && it.name.startsWith("voxy") && it.name.endsWith(".jar") && it.name != "voxy.jar" }
                ?.forEach {
                    logger.lifecycle("Replacing stale ${it.name} in ${runModsDir.asFile}")
                    it.delete()
                }
        }
    }

    tasks.matching { it.name == "runClient" }.configureEach {
        dependsOn(installVoxy)
    }
} else if (!voxyInRunMods) {
    logger.warn(
        "No Voxy for ${sc.current.version}; set voxy_version, or drop a jar in libs/voxy.jar"
    )
}

tasks.named("compileJava") {
    dependsOn(tasks.named("compileVoxyStubsJava"))
}

loom {
    runs {
        named("client") {
            runDirectory.set(clientRunDir)
            // Only jump into a world that already exists for this version.
            if (clientRunDir.dir("saves/New World/level.dat").asFile.exists()) {
                programArguments.addAll("--quickPlaySingleplayer", "New World")
            }
        }
    }
}

val javaVersion = if (sc.current.parsed >= "26") 25 else 21

tasks.processResources {
    val modVersion = project.version.toString()
    val mcDep = prop("minecraft_dependency")
    val neoforgeDep = prop("neoforge_dependency")
    val neoforgeMcDep = prop("neoforge_minecraft_dependency")

    inputs.property("version", modVersion)
    inputs.property("minecraft_dependency", mcDep)
    inputs.property("neoforge_dependency", neoforgeDep)
    inputs.property("neoforge_minecraft_dependency", neoforgeMcDep)
    inputs.property("java_version", javaVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion,
                "minecraft_dependency" to mcDep,
                "java_version" to javaVersion
            )
        )
    }

    // Placeholder FML metadata so NeoForge reports the missing Connector dependency instead of
    // silently ignoring the jar; Connector loads the mod as Fabric from the same file.
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            mapOf(
                "version" to modVersion,
                "neoforge_dependency" to neoforgeDep,
                "neoforge_minecraft_dependency" to neoforgeMcDep
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.encoding = "UTF-8"
    options.compilerArgs.add("-AdisableTargetValidator=true")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    withSourcesJar()
}

tasks.jar {
    exclude("net/caffeinemc/mods/sodium/api/config/**")

    val archivesBaseName = prop("archives_base_name")
    listOf("LICENSE", "COPYING").forEach { name ->
        from(rootProject.file(name)) {
            rename { "${it}_${archivesBaseName}" }
        }
    }
}

val collectJars = tasks.register<Copy>("collectJars") {
    group = "build"
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
}

tasks.named("build") {
    finalizedBy(collectJars)
}
