import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // Specify the Kotlin JVM plugin with a version
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Add the serialization plugin with the same Kotlin version
    kotlin("plugin.serialization") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Compose for Desktop
    implementation(compose.desktop.currentOs)

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // MongoDB Kotlin Driver with coroutine support
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")

    // KMongo coroutine extension (needed for descending, etc.)
    implementation("org.litote.kmongo:kmongo-coroutine:4.11.0")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("it.skrape:skrapeit:1.2.2")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DesktopApp"
            packageVersion = "1.0.0"
        }
    }
}