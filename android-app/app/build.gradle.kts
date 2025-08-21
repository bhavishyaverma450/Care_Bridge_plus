plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    signingConfigs {
        create("release") {
            keyAlias = "mykey"
            storePassword = "yash@1"
            keyPassword = "yash@1"
            storeFile = file("C:\\Users\\yashw\\Downloads\\New folder\\my-release-key.jks")
        }
    }
    namespace = "com.example.carebridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.carebridge"
        minSdk = 23
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.firebase.database)
    implementation(libs.recyclerview)
    implementation(libs.fragment)
    implementation(libs.generativeai)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.zxing.android.embedded)
    implementation(libs.cardview)


}


