plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kapt)  // ✅ For Data Binding if needed
    alias(libs.plugins.ksp)   // ✅ For Hilt
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    id("maven-publish")
}

android {
    namespace = "com.indiedev.networking"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

configure<PublishingExtension> {
    publications {
        // You can register the publications using a lambda with 'register'
        register<MavenPublication>("release") {
            // Use the afterEvaluate block for Android components
            afterEvaluate {
                from(components["release"])
            }

            groupId = "com.github.indestudio"
            artifactId = "networkingKit-release"
            version = "1.0.0"
        }

        // It is generally recommended to only publish the 'release' variant on JitPack.
        // Publishing 'debug' variants is usually unnecessary for public libraries.
        register<MavenPublication>("debug") {

            afterEvaluate {
                from(components["debug"])
            }

            groupId = "com.github.indestudio"
            artifactId = "networkingKit-debug"
            version = "1.0.0"
        }
    }
}



dependencies {
    implementation(libs.retrofit) {
        exclude(group = "com.squareup.okhttp3", module = "library")
    }

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    
    // Moshi for JSON parsing
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    debugImplementation(libs.chucker.debug)
    releaseImplementation(libs.chucker.release)

    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.certificate.transparency)

    debugImplementation(libs.flippernetworkplugin)
    debugImplementation(libs.flipper)
    debugImplementation(libs.soLoader)
    releaseImplementation(libs.flipper.noop)

    implementation(libs.hilt.android.core)
    ksp(libs.hilt.compiler)

    implementation(libs.coroutines.core)
    implementation(libs.android.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.json.simple) {
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }

    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation(libs.bundles.mockito)
    testImplementation(libs.core.testing)
    testImplementation(libs.retrofit.mock)
    testImplementation(libs.junit.jupiterapi)
    testImplementation(libs.turbine)
    testImplementation(libs.json)
    testRuntimeOnly(libs.junit.jupiterEngine)
    testImplementation(libs.junit.jupiterParams)
    testCompileOnly(libs.annotations)

    androidTestImplementation(libs.junit.ext)
}