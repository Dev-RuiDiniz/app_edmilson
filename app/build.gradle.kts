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

fun readStringConfig(key: String, defaultValue: String): String {
    val value = (project.findProperty(key) as String?)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: defaultValue
    return value
}

fun readPositiveLongConfig(key: String, defaultValue: Long): Long {
    val rawValue = (project.findProperty(key) as String?)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: return defaultValue
    return rawValue.toLongOrNull()?.takeIf { it > 0 } ?: defaultValue
}

fun asBuildConfigString(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
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
        minSdk = 21
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "API_BASE_URL",
            asBuildConfigString(readStringConfig("API_BASE_URL", "https://hotspot1.edmilsonti.com.br"))
        )
        buildConfigField(
            "String",
            "API_TV_CONTENT_PATH_TEMPLATE",
            asBuildConfigString(
                readStringConfig(
                    "API_TV_CONTENT_PATH_TEMPLATE",
                    "api/tv/propagandas?codigo={code}&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a"
                )
            )
        )
        buildConfigField(
            "String",
            "API_TV_REGISTER_DISPLAY_PATH_TEMPLATE",
            asBuildConfigString(
                readStringConfig(
                    "API_TV_REGISTER_DISPLAY_PATH_TEMPLATE",
                    "api/tv/registrar-exibicao?id={id}&codigo={code}&api_key=TV56beafcbe547ac8d6b4a95685efb2dc39b7b260fb645b55a"
                )
            )
        )
        buildConfigField(
            "long",
            "TV_DEFAULT_DISPLAY_DURATION_SECONDS",
            "${readPositiveLongConfig("TV_DEFAULT_DISPLAY_DURATION_SECONDS", 30)}L"
        )
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
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
