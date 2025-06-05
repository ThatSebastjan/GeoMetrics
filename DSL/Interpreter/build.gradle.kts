plugins {
    kotlin("jvm") version "2.1.10"
}

group = "com.my_app"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("io.github.dellisd.spatialk:geojson:0.3.0")
    implementation("io.github.dellisd.spatialk:turf:0.3.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}