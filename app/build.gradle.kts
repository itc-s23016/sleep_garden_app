plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // KSP を明示（Kotlin 2.0.21 用。Kotlin版が違う場合は番号を合わせてください）
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"

    // Room Gradle Plugin（スキーマ出力設定はこちらで）
    id("androidx.room") version "2.6.1"

}

android {
    namespace = "com.example.sleep_garden"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sleep_garden"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
}

dependencies {
    // --- Compose 基本 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.compose.foundation.layout)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // --- Room（KSPに統一） ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    // ★ kapt は使わない（入れない）
}

room {
    // スキーマJSONの出力先（app/schemas フォルダを作っておく）
    schemaDirectory("$projectDir/room_schemas")
}

// ★ ここに ksp { arg("room.schemaLocation", ...) } は書かない
