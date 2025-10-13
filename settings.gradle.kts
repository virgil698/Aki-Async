// TODO: change this to your plugin name
rootProject.name = "Aki-Async"
pluginManagement {
    repositories {
        maven("https://repo.leavesmc.org/releases") {
            name = "leavesmc-releases"
        }
        maven("https://repo.leavesmc.org/snapshots") {
            name = "leavesmc-snapshots"
        }
        gradlePluginPortal()
    }
}