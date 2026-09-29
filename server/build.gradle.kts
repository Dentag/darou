plugins {
    application
    kotlin("jvm")
}
dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.32")
    implementation("io.ktor:ktor-server-netty:3.6.0")
    implementation("io.ktor:ktor-server-websockets:3.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("dev.dentag.darou.server.MainKt")
}
