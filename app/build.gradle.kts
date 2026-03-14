import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "com.lhzkml.codestudio"
    compileSdk {
        version = release(36)
    }

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "com.lhzkml.codestudio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release-key.jks")
            storePassword = "codestudio123"
            keyAlias = "codestudio"
            keyPassword = "codestudio123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// 排除已废弃的 Kotlin Stdlib JDK7/JDK8（内容已合并到 kotlin-stdlib 2.2.0）
configurations.all {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    // 排除重复的 annotations 依赖
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

dependencies {
    implementation(project(":jasmine-core:prompt:prompt-executor"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    
    // Markwon - Markdown 渲染库
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:syntax-highlight:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")  // 删除线
    implementation("io.noties.markwon:ext-tables:4.6.2")  // 表格
    implementation("io.noties.markwon:ext-tasklist:4.6.2")  // 任务列表
    implementation("io.noties.markwon:ext-latex:4.6.2")  // LaTeX 数学公式
    implementation("io.noties.markwon:html:4.6.2")  // HTML 支持
    implementation("io.noties.markwon:image:4.6.2")  // 图片支持
    implementation("io.noties.markwon:linkify:4.6.2")  // 自动链接
    implementation("io.noties.markwon:inline-parser:4.6.2")  // 内联解析器（LaTeX 需要）
    implementation("io.noties:prism4j:2.0.0")
    
    kapt(libs.hilt.compiler)
    kapt(libs.kotlin.metadata.jvm)
    ksp(libs.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}