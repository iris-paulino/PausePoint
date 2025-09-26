
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
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.prismappsau.screengo"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.prismappsau.screengo"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 2
        versionName = "1.0"
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
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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