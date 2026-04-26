import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

val localProps = File(rootDir, "local.properties").inputStream().use {
    Properties().apply { load(it) }
}

android {
    namespace = "com.luna.budgetapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.luna.budgetapp"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "PUSHER_APP_ID",
            "\"${localProps["PUSHER_APP_ID"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_KEY",
            "\"${localProps["PUSHER_KEY"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_SECRET",
            "\"${localProps["PUSHER_SECRET"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_CLUSTER",
            "\"${localProps["PUSHER_CLUSTER"]}\""
        )
        buildConfigField(
            "String",
            "PUSHER_API_KEY",
            "\"${localProps["PUSHER_API_KEY"]}\""
        )
        buildConfigField(
            "String",
            "LOCAL_BACKEND_URL",
            "\"${localProps["LOCAL_BACKEND_URL"]}\""
        )
    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            val propertiesFile = rootProject.file("keystore.properties")
            if (propertiesFile.exists()) {
                properties.load(propertiesFile.inputStream())
            }

            storeFile = file(properties.getProperty("RELEASE_STORE_FILE"))
            storePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = properties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.foundation)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Standard JUnit 4
    testImplementation(libs.junit)

    // Coroutines Testing (Required for viewModelScope)
    testImplementation(libs.kotlinx.coroutines.test)

    // MockK (For mocking your UseCases and Repository)
    testImplementation(libs.mockk)

    // Turbine (For easy StateFlow testing)
    testImplementation(libs.turbine)

    // Google Truth (For readable assertions)
    testImplementation(libs.truth)

    // Arch Core Testing (Required for LiveData or InstantTaskExecutorRule)
    testImplementation(libs.androidx.core.testing)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Pusher
    implementation(libs.pusher.java.client)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // JWT Decode
    implementation(libs.jwt.decode)

    // Pagination
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    testImplementation(libs.androidx.paging.common)
    testImplementation(libs.androidx.paging.testing)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)

    // Google Authentication
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
