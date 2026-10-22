plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rkawa.kakunote"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.rkawa.kakunote"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        // 端末で動かすテストは、共通テストケース（testcases/）を assets として読む
        getByName("androidTest").assets.directories.add(rootProject.file("testcases").path)
    }
    testOptions {
        unitTests.all {
            // 漢字の DB（src/main/assets/kanji.sqlite）と共通テストケース（リポジトリ直下の testcases/）は、
            // iOS のリポジトリからコピーしたもの。テストはこの 2 つを直接読む
            it.systemProperty("kakunote.kanjiDb", file("src/main/assets/kanji.sqlite").absolutePath)
            it.systemProperty("kakunote.testcases", rootProject.file("testcases").absolutePath)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // 単体テスト（JVM）では Android の SQLite と org.json が使えないので、代わりのものを使う
    testImplementation(libs.json)
    testImplementation(libs.sqlite.jdbc)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose の UI テストが使う Espresso は、古い版だと新しい Android（API 37）で動かないので、版を上げる
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
