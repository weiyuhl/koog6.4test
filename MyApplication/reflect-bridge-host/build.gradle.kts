plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.myapplication.reflectbridge.host.ReflectBridgeHostKt")
}

dependencies {
    implementation(project(":reflect-bridge-protocol"))
    implementation(libs.koog.agents.tools)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}