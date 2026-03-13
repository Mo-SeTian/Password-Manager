plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.mosetian.passwordmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mosetian.passwordmanager"
        minSdk = 28
        targetSdk = 35
        versionCode = 18
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val releaseStoreFilePath = System.getenv("PM_RELEASE_STORE_FILE")?.takeIf { it.isNotBlank() }
    val releaseSigningAvailable = releaseStoreFilePath != null

    signingConfigs {
        create("release") {
            if (releaseSigningAvailable) {
                storeFile = rootProject.file(releaseStoreFilePath!!)
                storePassword = System.getenv("PM_RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("PM_RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("PM_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // 使用正式包名；若配置了签名则使用 release 签名，否则回退到 debug 签名
            signingConfig = if (releaseSigningAvailable) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.google.material)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
