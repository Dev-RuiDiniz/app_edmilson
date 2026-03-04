import org.gradle.api.GradleException
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun readSigningValue(key: String): String? {
    return keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
}

val releaseStoreFile = readSigningValue("RELEASE_STORE_FILE")
val releaseStorePassword = readSigningValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = readSigningValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = readSigningValue("RELEASE_KEY_PASSWORD")

val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

val runningReleaseTask = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true) || taskName.contains("bundle", ignoreCase = true)
}

if (runningReleaseTask && !hasReleaseSigning) {
    throw GradleException(
        "Release signing is not configured. " +
            "Provide keystore.properties or RELEASE_* environment variables."
    )
}

android {
    namespace = "com.example.app_edmilson"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.app_edmilson"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://hotspot1.edmilsonti.com.br/api/\"")
        buildConfigField("String", "PAIRING_URL", "\"https://hotspot1.edmilsonti.com.br/tv-pair\"")
        buildConfigField("int", "API_POLL_SECONDS", "15")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
