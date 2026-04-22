plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    kotlin("plugin.serialization") version "2.0.21"
    id ("kotlin-parcelize")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

val mapsApiKey = project.findProperty("MAPS_API_KEY") as String? ?: ""
val placesApiKey = project.findProperty("PLACES_API_KEY") as String? ?: ""

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKey\"")
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

    viewBinding {
        enable = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
    ignoreList.add("keyToIgnore")
    ignoreList.add("sdk.*")
}

dependencies {
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.ui.test.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.firebase.messaging)

    //http
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation(libs.play.services.location)
    implementation(libs.firebase.storage.ktx)


    val nav_version = "2.8.5"
    implementation("androidx.navigation:navigation-compose:$nav_version")
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Material 3 Core functionality and modules
    implementation ("com.maxkeppeler.sheets-compose-dialogs:core:1.2.0")
    // CALENDAR
    implementation ("com.maxkeppeler.sheets-compose-dialogs:calendar:1.2.0")
    // Clock dialog
    implementation ("com.maxkeppeler.sheets-compose-dialogs:clock:1.2.0")

    implementation ("com.google.android.libraries.places:places:3.3.0")

    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation ("androidx.compose.ui:ui-tooling:1.7.6")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // this is for Dialog toolbar
    implementation("com.github.skydoves:balloon:1.6.11")

    // those for Firebase storage for uploding image
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    implementation(libs.play.services.auth)
    implementation(libs.circleimageview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.library)
    implementation (libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
