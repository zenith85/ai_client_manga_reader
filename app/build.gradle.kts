plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.0" apply true // Apply Kotlin plugin if using Kotlin
}

android {
    namespace = "com.example.aireader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aireader"
        minSdk = 26 // Minimum SDK version should be 26 due to adaptive icons
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Kotlin-specific options (if using Kotlin)
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation ("com.itextpdf:itextpdf:5.5.13.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation(libs.filament.android)
    implementation(libs.vision.common)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.play.services.mlkit.text.recognition)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")
    implementation("com.rmtheis:tess-two:9.1.0")
    implementation("org.opencv:opencv:4.10.0")

    // Remove duplicate dependencies
}
