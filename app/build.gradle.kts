import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun apiKey(environment: String, label: String): String =
    providers.environmentVariable(environment).orNull
        ?: rootProject.file("api").takeIf { it.exists() }?.readLines()?.firstOrNull { it.startsWith(label) }
            ?.substringAfter('=')?.trim()?.removeSurrounding("'")?.removeSurrounding("\"")
        ?: ""

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

android {
    namespace = "com.roadahead"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.roadahead"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
        buildConfigField("String", "SIGNAL_KEY", quoted(apiKey("ROAD_AHEAD_SIGNAL_KEY", "신호등")))
        buildConfigField("String", "DISASTER_KEY", quoted(apiKey("ROAD_AHEAD_DISASTER_KEY", "재난")))
        buildConfigField("String", "WEATHER_KEY", quoted(apiKey("ROAD_AHEAD_WEATHER_KEY", "기상")))
        buildConfigField("String", "SEOUL_KEY", quoted(apiKey("ROAD_AHEAD_SEOUL_KEY", "서울")))
    }
    signingConfigs {
        if (keystoreProperties != null) create("release") {
            storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (keystoreProperties != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    useLibrary("android.car")
    testOptions.unitTests.all { it.useJUnitPlatform() }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core"))
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi)
    implementation(libs.pleos.tts)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
