import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aptirise.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aptirise.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 14
        versionName = "1.1.3_test"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    val keystorePropertiesFile = rootProject.file("release-keystore.properties")

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {

                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                val storeFilePath = keystoreProperties.getProperty("storeFile")
                val storePasswordValue = keystoreProperties.getProperty("storePassword")
                val keyAliasValue = keystoreProperties.getProperty("keyAlias")
                val keyPasswordValue = keystoreProperties.getProperty("keyPassword")

                require(!storeFilePath.isNullOrBlank()) {
                    "ERROR: storeFile is missing in release-keystore.properties"
                }

                require(!storePasswordValue.isNullOrBlank()) {
                    "ERROR: storePassword is missing in release-keystore.properties"
                }

                require(!keyAliasValue.isNullOrBlank()) {
                    "ERROR: keyAlias is missing in release-keystore.properties"
                }

                require(!keyPasswordValue.isNullOrBlank()) {
                    "ERROR: keyPassword is missing in release-keystore.properties"
                }

                storeFile = rootProject.file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {

        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }

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
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Jetpack Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // DataStore & Coroutines
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // SplashScreen
    implementation(libs.androidx.core.splashscreen)

    // Firebase (BoM)
    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-config")

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}