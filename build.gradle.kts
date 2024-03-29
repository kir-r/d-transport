import java.net.*

plugins {
    kotlin("multiplatform")
    id("de.undercouch.download")
    id("com.github.hierynomus.license")
    `maven-publish`
}

val coroutinesVersion: String by extra
val drillLoggerVersion: String by extra
val immutableVersion: String by extra
val libwebsocketVersion: String by extra


val scriptUrl: String by extra

apply(from = "$scriptUrl/git-version.gradle.kts")


repositories {
    mavenLocal()
    mavenCentral()
    apply(from = "$scriptUrl/maven-repo.gradle.kts")
}

val prepareStaticLib by tasks.creating(de.undercouch.gradle.tasks.download.Download::class) {
    group = "build"
    src(setOf(
        "libwebsockets-linuxX64" to "a",
        "libwebsockets-macosX64" to "a",
        "libwebsockets-mingwX64" to "a"
    ).map { (presetName, ext) -> "https://github.com/Drill4J/libwebsockets/releases/download/v$libwebsocketVersion/$presetName.$ext" })
    dest("${buildDir}/nativelib/build")

}

val downloadHeaders by tasks.creating(de.undercouch.gradle.tasks.download.Download::class) {
    src("https://github.com/Drill4J/libwebsockets/releases/download/v$libwebsocketVersion/headers.zip")
    dest("${buildDir}/nativelib/headers.zip")
}

val downloadAndUnzipHeaders by tasks.creating {
    dependsOn(downloadHeaders)
    doFirst {
        copy {
            from(zipTree("${buildDir}/nativelib/headers.zip"))
            into("${buildDir}/nativelib")
        }
    }
}


configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")).with(module("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion-native-mt"))
    }
}

kotlin {

    targets {
        setOf(
            mingwX64(),
            macosX64(),
            linuxX64(),
        ).forEach {
            it.compilations["main"].addCInterop()
        }
    }
    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlin.ExperimentalStdlibApi")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$immutableVersion")
            }
        }
        val commonNative by creating {
            dependsOn(commonMain)
        }
        val mingwX64Main by getting {
            dependsOn(commonNative)
        }
        val linuxX64Main by getting {
            dependsOn(commonNative)
        }
        val macosX64Main by getting {
            dependsOn(commonNative)
        }

    }
}


fun org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation.addCInterop() {
    val create = cinterops.create("websocket_bindings")
    create.includeDirs(
        rootProject
            .file("build")
            .resolve("nativelib")
            .resolve("build")
            .resolve("include")
    )
    tasks.getByName(create.interopProcessingTaskName).dependsOn(prepareStaticLib, downloadAndUnzipHeaders)
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)

