import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val mobileConfig = rootProject.layout.projectDirectory.file(".local/mobile.properties")
val serverOrigin = providers.fileContents(mobileConfig).asText.map { text ->
    Properties().apply { load(text.reader()) }
        .getProperty("serverOrigin", "https://example.invalid")
}.orElse("https://example.invalid")
val serverOriginLiteral = serverOrigin.map {
    "\"" + it.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r") + "\""
}

android {
    namespace = "dev.dentag.darou"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.dentag.darou"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SERVER_ORIGIN", serverOriginLiteral.get())
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(project(":shared"))
    implementation(project(":shared-ui"))
}
