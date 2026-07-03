plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.feystray.geotagger"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "org.feystray.geotagger"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.2"
        buildConfigField("String", "GAPIKEY", "${project.findProperty("GMAPSAPI")}")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures.buildConfig = true
    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation (libs.camera.core)
    implementation (libs.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation(libs.google.material)
}