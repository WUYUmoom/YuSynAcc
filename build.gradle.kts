plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

group = "com.wuyumoom"

version = "1.0.0-SNAPSHOT"

loom { splitEnvironmentSourceSets() }

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "AliyunMaven"
        url = uri("https://maven.aliyun.com/repository/public")
    }
    maven { url = uri("https://nexus.wuyumoom.top:2026/repository/maven-public/") }
    maven { url = uri("https://maven.impactdev.net/repository/development/") }

    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "rosewood-repo"
        url = uri("https://repo.rosewooddev.io/repository/public/")
    }
}

dependencies {
    /*<-minecraft->*/
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    // mappings "net.fabricmc:yarn:1.21.1+build.3:v2"

    modCompileOnly("wuyumoom:yucore:1.6.6:YuCore@jar")
    /*<-cobblemon->*/

    /*<-spigot->*/
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    // PAPI
    compileOnly("me.clip:placeholderapi:2.11.6")
    // NBTAPI
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.14.2-SNAPSHOT")
    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    // PlayerPoints API
    compileOnly("org.black_ixx:playerpoints:3.2.5")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.named<ProcessResources>("processResources") {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
                "version" to (project.version as String).replace("-SNAPSHOT", ""),
                "author" to "WUYUmoom"
        )
    }
}

tasks.named<Test>("test") { useJUnitPlatform() }

kotlin { jvmToolchain(21) }
kotlin { jvmToolchain(21) }
