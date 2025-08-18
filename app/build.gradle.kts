plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bntsoft.toastmasters"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bntsoft.toastmasters"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
    
    // Room schema location
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")
    
    // Material3 Components
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
    
    // For backward compatibility
    implementation("androidx.compose.material:material:1.6.3")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-android-compiler:2.56.2")

    // Room
    val room_version = "2.7.2"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-paging:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    val lottieVersion = "6.0.0"
    implementation("com.airbnb.android:lottie:$lottieVersion")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Hilt dependencies
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Additional UI components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.12.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
}