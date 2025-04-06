import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "dev.uday"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha11")

}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ProjectEXO_Kotlin"
            packageVersion = "1.0.0"
        }
    }
}

tasks.register("showEnv") {
    doLast {
        println("OS: ${System.getProperty("os.name")}")
        println("Architecture: ${System.getProperty("os.arch")}")
        println("Java Version: ${System.getProperty("java.version")}")
    }
}
