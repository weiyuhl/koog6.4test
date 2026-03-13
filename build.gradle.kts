plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
}

buildscript {
    dependencies {
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.6")
    }
}
