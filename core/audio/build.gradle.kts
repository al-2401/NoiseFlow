plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Deliberately a plain Kotlin/JVM module, not an Android library.
// The DSP is the most valuable and most regression-prone part of the product,
// so it must be unit-testable on the JVM without an emulator. Everything
// Android-specific (AudioTrack, service, notifications) lives in :core:playback.

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    maxHeapSize = "1g"
}
