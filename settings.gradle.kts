rootProject.name = "transport"

val scriptUrl: String by extra
apply(from = "$scriptUrl/maven-repo.settings.gradle.kts")

pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    val undercouchDownloaderVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("com.github.hierynomus.license") version licenseVersion
        id("de.undercouch.download") version undercouchDownloaderVersion
    }
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
