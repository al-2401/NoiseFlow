plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "app.noiseflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.noiseflow"
        // AudioTrack.PERFORMANCE_MODE_POWER_SAVING arrived in API 26 and it is
        // load-bearing for the battery budget, so 26 is the floor.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf(
            "en", "ru", "uk", "de", "es", "fr", "it", "pt-rBR", "pl", "tr",
        )
    }

    /**
     * Two stores, two builds. Google Play is the main channel; RuStore is the
     * second and must work on devices with no Google Play Services at all, so
     * anything GMS-shaped is confined to the `play` source set.
     */
    flavorDimensions += "store"
    productFlavors {
        create("play") {
            dimension = "store"
            isDefault = true
        }
        create("rustore") {
            dimension = "store"
            versionNameSuffix = "-rustore"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            // Pseudolocale: catches strings that overflow once translated,
            // long before the German translation lands.
            isPseudoLocalesEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.audio)
    implementation(projects.core.playback)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.i18n)
    implementation(projects.feature.player)
    implementation(projects.feature.mixer)
    implementation(projects.feature.presets)
    implementation(projects.feature.settings)
    implementation(projects.feature.paywall)
    implementation(projects.billing.api)

    "playImplementation"(projects.billing.play)
    "rustoreImplementation"(projects.billing.rustore)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    // GMS-independent on purpose: Crashlytics needs Play Services, which the
    // rustore build cannot assume. One reporter, identical data from both stores.
    implementation(libs.sentry.android)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
