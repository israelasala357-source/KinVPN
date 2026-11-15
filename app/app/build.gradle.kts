plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kinvpn.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.kinvpn.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20230706")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
