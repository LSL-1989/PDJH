import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 签名配置须在 android {} 外读取：块内 java 会被 DSL 遮蔽，导致 java.util 无法解析
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.exists()
if (hasReleaseKeystore) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.hospital.queue.shell"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hospital.queue.shell"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        val serverBase = project.findProperty("SERVER_BASE")?.toString()
            ?: "http://queue.hospital.local"
        buildConfigField("String", "DEFAULT_SERVER_BASE", "\"$serverBase\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "shell"
    productFlavors {
        create("screen") {
            dimension = "shell"
            applicationIdSuffix = ".screen"
            versionNameSuffix = "-screen"
            resValue("string", "app_name", "候诊大屏壳")
            buildConfigField("String", "TERMINAL_TYPE", "\"SCREEN\"")
            buildConfigField("String", "WEB_PATH", "\"/screen/\"")
            manifestPlaceholders["screenOrientation"] = "landscape"
        }
        create("clinic") {
            dimension = "shell"
            applicationIdSuffix = ".clinic"
            versionNameSuffix = "-clinic"
            resValue("string", "app_name", "门边屏壳")
            buildConfigField("String", "TERMINAL_TYPE", "\"CLINIC\"")
            buildConfigField("String", "WEB_PATH", "\"/clinic/\"")
            manifestPlaceholders["screenOrientation"] = "portrait"
        }
        create("checkin") {
            dimension = "shell"
            applicationIdSuffix = ".checkin"
            versionNameSuffix = "-checkin"
            resValue("string", "app_name", "签到壳")
            buildConfigField("String", "TERMINAL_TYPE", "\"CHECKIN\"")
            buildConfigField("String", "WEB_PATH", "\"/checkin/\"")
            manifestPlaceholders["screenOrientation"] = "portrait"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
}
