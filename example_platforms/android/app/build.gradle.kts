plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val moduleAndroid = "${rootDir}/../../android"
val moduleSources = "$moduleAndroid/src/main/java/sincpro/expo/printer"

android {
    namespace = "sincpro.example.app_printer_sdk"
    compileSdk = 36

    defaultConfig {
        applicationId = "sincpro.example.app_printer_sdk"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Same as an Expo SDK 58 production build: R8 on, with the module's Bixolon keep rules.
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "$moduleAndroid/proguard-rules.pro"
            )
        }
    }
    sourceSets {
        getByName("main") {
            // Context: compiles the module's native layers directly (everything except the Expo
            // entrypoints), so this app always tests the exact code that ships in the package.
            java.srcDirs(
                "$moduleSources/domain",
                "$moduleSources/adapters",
                "$moduleSources/services",
                "$moduleSources/infrastructure"
            )
            jniLibs.srcDir("$moduleAndroid/src/main/jniLibs")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    // ====== Bixolon Label SDK, shared with the Expo module (android/libs) ======
    implementation(fileTree("$moduleAndroid/libs") { include("*.jar") })

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ====== App Dependencies ======
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // View-based UI (for test runner)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
