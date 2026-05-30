plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "ru.misterpotz.listly"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.misterpotz.listly"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"http://158.160.251.150:8080\"")

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

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.elmslie.android)

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.retrofit)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp) // required for http/https

    implementation(libs.dagger)
    ksp(libs.dagger.compiler) // Dagger compiler


    // для напоминалок
    implementation("androidx.work:work-runtime-ktx:2.9.0")              // WorkManager
    implementation("androidx.datastore:datastore-preferences:1.1.1")   // DataStore (local persistence)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
