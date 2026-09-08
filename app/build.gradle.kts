plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "app.orionmd.digitalwallet"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.orionmd.digitalwallet"
        minSdk = 26
        targetSdk = 34
        // versionCode/versionName are bumped automatically by the build workflow.
        // Do not hand-edit these two lines' values without also updating changelog.txt.
        versionCode = 16
        versionName = "2.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // ML Kit's on-device OCR model ships a native library per CPU architecture
        // (arm64-v8a/armeabi-v7a/x86/x86_64), which alone roughly quadruples APK size if all
        // four are bundled. Which architectures get built at all - just the two real-phone ones,
        // 64-bit and 32-bit ARM, skipping emulator-only x86/x86_64 - is controlled below by
        // splits.abi.include, not here (Gradle rejects setting both).
    }

    // Real release signing, read entirely from environment variables so the keystore itself
    // and its passwords never touch source control. Set locally (for a one-off manual release
    // build) or, normally, injected by build-apk.yml from GitHub Actions secrets. When these
    // aren't set - any ad-hoc/local build, including this sandbox - `release` quietly falls
    // back to debug signing, same as before, so nothing here can break a build that doesn't
    // have the real keystore available.
    val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // One APK per architecture instead of a single APK holding both native libraries - each
    // resulting file is roughly half the size, which matters for sideloading (email/chat/drive
    // size limits) even though this app was never going to be distributed through Play. Install
    // whichever one matches your device: arm64-v8a for anything modern, armeabi-v7a for an
    // older/budget 32-bit device. Not sure which you have? arm64-v8a covers virtually every
    // phone sold in the last several years.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // "Finances at a Glance": on-device OCR (Google ML Kit, free/offline-capable, no cloud
    // upload of statement contents) + real text extraction from digital-native statement PDFs.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // Turns ML Kit's Task<T> API into a suspend function (kotlinx's Task.await()).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
