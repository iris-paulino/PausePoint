
import java.util.Properties
import java.io.FileInputStream

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("com.journeyapps:zxing-android-embedded:4.3.0")
                implementation("com.google.zxing:core:3.5.2")
                implementation("androidx.work:work-runtime-ktx:2.9.0")
                // Splash screen API for Android 12+
                implementation("androidx.core:core-splashscreen:1.0.1")
                // Compose UI used by CongratulationsOverlayActivity
                implementation(compose.material)
                implementation(compose.foundation)
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.luminoprisma.scrollpause"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.luminoprisma.scrollpause"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 11
        versionName = "6.2.5"
    }
    
    signingConfigs {
        create("release") {
            val file = rootProject.file("androidApp/keystore.properties")
            if (file.exists()) {
                val props = Properties().apply {
                    load(FileInputStream(file))
                }
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
                storeFile = file(props["storeFile"] as String)
                storePassword = props["storePassword"] as String
            }
        }
    }
    
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

}