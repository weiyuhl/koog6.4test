plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.lhzkml.jasmine.core.assistant.tools"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":jasmine-core:prompt:prompt-model"))
    implementation(project(":jasmine-core:agent:agent-tools"))
    implementation(project(":jasmine-core:assistant:assistant-memory"))
    implementation(project(":jasmine-core:assistant:assistant-scheduler"))
    implementation(project(":jasmine-core:assistant:assistant-email"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
}
