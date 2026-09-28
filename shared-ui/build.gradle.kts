import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    android {
        namespace = "dev.dentag.darou.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        androidResources.enable = true
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "DarouUI"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.resources)
            implementation(libs.compose.runtime)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":shared"))
        }
    }
}

compose.resources {
    packageOfResClass = "dev.dentag.darou.ui.resources"
}
