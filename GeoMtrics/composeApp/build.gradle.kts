import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localPropsFile = rootProject.file("gradle.properties.local")
if (localPropsFile.exists()) {
    val localProps = Properties().apply { localPropsFile.reader().use { load(it) } }
    localProps.forEach { (k, v) ->
        project.extensions.extraProperties.set(k.toString(), v)
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("com.mapbox.maps:android-ndk27:11.17.1")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.geometrics.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.geometrics.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val mapboxToken = (findProperty("MAPBOX_ACCESS_TOKEN") as String?)
            ?: System.getenv("MAPBOX_ACCESS_TOKEN")
            ?: error("MAPBOX_ACCESS_TOKEN is not set")

        resValue(
            "string",
            "mapbox_access_token",
            mapboxToken
        )    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)

    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
}

