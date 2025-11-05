plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
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

    // Compose BOM（他の compose-* は版指定なしでOK）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // rememberSnapFlingBehavior など
    implementation("androidx.compose.foundation:foundation")

    // ★ Compose Navigation 追加（これが無いと NavHost/composable/rememberNavController が解決できません）
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // （任意）非Compose UI を使わないなら下2つは削除可
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.11.0")

    // Google Fonts（必要なら）
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.media3.effect)

    // --- 端末DB: Room ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // --- テスト / デバッグ ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

room {
    // スキーマJSONの出力先（リポジトリにコミット推奨）
    schemaDirectory("$projectDir/schemas")
}