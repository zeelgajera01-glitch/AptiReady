import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.aptiready"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aptirise.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    val keystorePropertiesFile = rootProject.file("release-keystore.properties")
    val enableLiveAds = project.findProperty("enableLiveAds")?.toString()?.toBoolean() == true

    signingConfigs {
        create("release") {

            require(keystorePropertiesFile.exists()) {
                "ERROR: release-keystore.properties not found at: ${keystorePropertiesFile.absolutePath}"
            }

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
    buildTypes {
        debug {
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-4263244815223132~5490032720\"")
            buildConfigField("String", "NATIVE_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")

            manifestPlaceholders["admobAppId"] =
                "ca-app-pub-4263244815223132~5490032720"
        }

        release {
            signingConfig = signingConfigs.getByName("release")

            // Defaults to false unless explicitly built with -PenableLiveAds=true
            buildConfigField(
                "boolean",
                "ADS_ENABLED",
                enableLiveAds.toString()
            )

            buildConfigField(
                "String",
                "ADMOB_APP_ID",
                "\"ca-app-pub-4263244815223132~5490032720\""
            )

            buildConfigField(
                "String",
                "NATIVE_AD_UNIT_ID",
                "\"ca-app-pub-4263244815223132/9976072648\""
            )

            buildConfigField(
                "String",
                "BANNER_AD_UNIT_ID",
                "\"ca-app-pub-4263244815223132/1518998127\""
            )

            buildConfigField(
                "String",
                "INTERSTITIAL_AD_UNIT_ID",
                "\"ca-app-pub-4263244815223132/4167458178\""
            )

            buildConfigField(
                "String",
                "REWARDED_AD_UNIT_ID",
                "\"ca-app-pub-4263244815223132/3765887514\""
            )

            manifestPlaceholders["admobAppId"] =
                "ca-app-pub-4263244815223132~5490032720"

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
    implementation(libs.androidx.lifecycle.process)

    // SplashScreen
    implementation(libs.androidx.core.splashscreen)

    // Firebase (BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Google Mobile Ads & UMP
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
