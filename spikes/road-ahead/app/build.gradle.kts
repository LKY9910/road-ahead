plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.roadahead.spike"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.roadahead.spike"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    useLibrary("android.car")
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi)
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation(libs.kotlinx.coroutines.core)
}
