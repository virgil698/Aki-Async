@file:Suppress("SpellCheckingInspection")

import org.leavesmc.leavesPluginJson
import xyz.jpenilla.runtask.service.DownloadsAPIService
import xyz.jpenilla.runtask.service.DownloadsAPIService.Companion.registerIfAbsent

plugins {
    java
    alias(libs.plugins.leavesweightUserdev)
    alias(libs.plugins.shadowJar)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.resourceFactory)
    alias(libs.plugins.accessWiden)
}

// TODO: change this to your plugin group
group = "com.example"
// TODO: change this to your plugin version
version = "1.0.0-SNAPSHOT"

// please check https://docs.papermc.io/paper/dev/plugin-yml/ and https://docs.papermc.io/paper/dev/getting-started/paper-plugins/
val pluginJson = leavesPluginJson {
    // INFO: name and version defaults to project name and version
    // TODO: change this to your main class
    main = "com.example.plugin.TemplatePlugin"
    // TODO: change this to your name
    authors.add("YourName")
    // TODO: change this to your plugin description
    description = "leaves template plugin"
    // TODO: support or not is decided by you
    foliaSupported = false
    apiVersion = libs.versions.leavesApi.extractMCVersion()
    // TODO: if your logic can work without mixin, can use `features.optional.add("mixin")`
    features.required.add("mixin")
    mixin.apply {
        // TODO: replace this to your mixin package name
        packageName = "com.example.plugin.mixin"
        // TODO: replace this to your access widener file name
        accessWidener = "leaves-template-plugin.accesswidener"
        // TODO: replace this to your mixin configs name
        mixins.add("leaves-template-plugin.mixins.json")
    }
    // TODO: add your plugin dependencies
    // please check https://docs.papermc.io/paper/dev/getting-started/paper-plugins/#dependency-declaration
    // e.g.,
    // dependencies.bootstrap(
    //     name = "some deps",
    //     load = LeavesPluginJson.Load.BEFORE // or AFTER
    // )
}

val runServerPlugins = runPaper.downloadPluginsSpec {
    // TODO: add plugins you want when run dev server
    // e.g.,
    // modrinth("carbon", "2.1.0-beta.21")
    // github("jpenilla", "MiniMOTD", "v2.0.13", "minimotd-bukkit-2.0.13.jar")
    // hangar("squaremap", "1.2.0")
    // url("https://download.luckperms.net/1515/bukkit/loader/LuckPerms-Bukkit-5.4.102.jar")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://modmaven.dev/") {
        name = "modmaven"
    }
    maven("https://repo.leavesmc.org/releases/") {
        name = "leavesmc-releases"
    }
    maven("https://repo.leavesmc.org/snapshots/") {
        name = "leavesmc-snapshots"
    }
    mavenLocal()
}

sourceSets {
    create("mixin") {
        java.srcDir("mixin/java")
        resources.srcDir("mixin/resources")
    }

    main {
        resourceFactory {
            factories(pluginJson.resourceFactory())
        }
    }
}
val mixinSourceSet: SourceSet = sourceSets["mixin"]

dependencies {
    apply `plugin dependencies`@{
        // TODO: your plugin deps here
    }

    apply `api and server source`@{
        compileOnly(libs.leavesApi)
        paperweight.devBundle(libs.leavesDevBundle)
    }

    apply `mixin dependencies`@{
        compileOnly(mixinSourceSet.output)
        mixinSourceSet.apply {
            val compileOnly = compileOnlyConfigurationName
            val annotationPreprocessor = annotationProcessorConfigurationName

            annotationPreprocessor(libs.mixinExtras)
            compileOnly(libs.mixinExtras)
            compileOnly(libs.spongeMixin)
            compileOnly(libs.mixinCondition)
            accessWiden(compileOnly(files(getMappedServerJar()))!!)
        }
    }
}

accessWideners {
    files.from(fileTree(mixinSourceSet.resources.srcDirs.first()) {
        include("*.accesswidener")
    })
}

tasks {
    runServer {
        downloadsApiService.set(leavesDownloadApiService())
        downloadPlugins.from(runServerPlugins)
        minecraftVersion(libs.versions.leavesApi.extractMCVersion())
        systemProperty("leavesclip.enable.mixin", true)
        systemProperty("file.encoding", Charsets.UTF_8.name())
    }

    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.forkOptions.memoryMaximumSize = "6g"

        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }

    named<JavaCompile>("compileMixinJava") {
        dependsOn("paperweightUserdevSetup")
        dependsOn(applyAccessWideners)
    }

    paperweightUserdevSetup {
        finalizedBy(applyAccessWideners)
    }

    shadowJar {
        from(mixinSourceSet.output)
        archiveFileName = "${project.name}-${version}.jar"
    }

    build {
        dependsOn(shadowJar)
    }
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

fun getMappedServerJar(): String = File(rootDir, ".gradle")
    .resolve("caches/paperweight/taskCache/mappedServerJar.jar")
    .path

fun Provider<String>.extractMCVersion(): String {
    val versionString = this.get()
    val regex = Regex("""^(1\.\d+(?:\.\d+)?)""")
    return regex.find(versionString)?.groupValues?.get(1)
        ?: throw IllegalArgumentException("Cannot extract mcVersion from $versionString")
}

fun leavesDownloadApiService(): Provider<out DownloadsAPIService> = registerIfAbsent(project) {
    downloadsEndpoint = "https://api.leavesmc.org/v2/"
    downloadProjectName = "leaves"
    buildServiceName = "leaves-download-service"
}