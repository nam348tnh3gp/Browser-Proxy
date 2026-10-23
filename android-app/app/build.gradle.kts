plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "TGFN.BrowserMini"
    compileSdk = 35

    defaultConfig {
        applicationId = "TGFN.BrowserMini"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
