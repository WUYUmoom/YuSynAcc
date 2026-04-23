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
    maven { url = uri("https://maven.shedaniel.me/") }
    maven { url = uri("https://maven.wispforest.io/releases") }
    maven { url = uri("https://maven.su5ed.dev/releases") }
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
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    modCompileOnly("io.wispforest:accessories-fabric:1.1.0-beta.7+1.21.1")
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
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

tasks.register<Exec>("deployToServer") {
    group = "deployment"
    description = "上传插件到远程服务器"
    
    dependsOn("remapJar")
    
    val jarFile = project.layout.buildDirectory.dir("libs").get().asFile.listFiles { file ->
        file.name.startsWith("YuSynAcc") && 
        file.name.endsWith(".jar") && 
        !file.name.contains("sources") && 
        !file.name.contains("shadow")
    }?.maxByOrNull { it.lastModified() }
    
    if (jarFile == null) {
        throw GradleException("未找到构建产物 JAR 文件")
    }
    
    val serverHost = "wuyumoom@202.189.4.208"
    val remotePath1 = "/mnt/1区/plugins/"
    val remotePath2 = "/mnt/2区/plugins/"
    
    // 使用 bash 脚本执行多个 scp 命令
    commandLine(
        "bash", "-c",
        "scp '${jarFile.absolutePath}' '$serverHost:$remotePath1' && " +
        "scp '${jarFile.absolutePath}' '$serverHost:$remotePath2'"
    )
    
    doFirst {
        println("准备上传: ${jarFile.name}")
        println("目标服务器: $serverHost")
        println("目标路径 1: $remotePath1")
        println("目标路径 2: $remotePath2")

    }
    
    doLast {
        println("上传完成!")
    }
}
tasks.named("build") {
    finalizedBy("deployToServer")
}

kotlin {jvmToolchain(21)}
