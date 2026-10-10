plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    idea
}

val modId = property("mod.id") as String

version = "${property("mod.version")}+${sc.current.version}"
group = property("mod.group") as String
base.archivesName = modId

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

neoForge {
    version = property("deps.neo_loader") as String

    findProperty("deps.parchment_mappings")?.let { mappings ->
        parchment {
            mappingsVersion = mappings as String
            minecraftVersion = property("deps.parchment_minecraft") as String
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory = rootProject.file("run")
        }
        register("server") {
            server()
            gameDirectory = rootProject.file("run")
            programArgument("--nogui")
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val modMetadata = mapOf(
    "minecraft_version_range" to property("mod.mc_compat"),
    "neo_version_range" to property("deps.neo_compat"),
    "loader_version_range" to property("deps.loader_compat"),
    "mod_id" to modId,
    "mod_name" to property("mod.name"),
    "mod_license" to property("mod.license"),
    "mod_version" to version,
    "mod_authors" to property("mod.authors"),
    "mod_description" to property("mod.description"),
).mapValues { it.value.toString() }

tasks {
    processResources {
        // Local copies: closures must not capture the build script for the configuration cache
        val props = modMetadata
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    jar {
        val id = modId
        from(rootProject.file("LICENSE")) { rename { "${it}_$id" } }
    }

    // Copies every version's jar into the root build/libs so CI can pick them up in one place
    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
