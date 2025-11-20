import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    id("com.google.protobuf") version "0.9.5"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.example.upitracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.upitracker"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // These properties will be passed in securely by the GitHub Actions workflow.
            // You can also place them in a local gradle.properties file for local builds.
            val storeFile = file(System.getProperty("android.injected.signing.store.file", "none"))
            if (storeFile.exists()) {
                this.storeFile = storeFile
                this.storePassword = System.getProperty("android.injected.signing.store.password")
                this.keyAlias = System.getProperty("android.injected.signing.key.alias")
                this.keyPassword = System.getProperty("android.injected.signing.key.password")
            }
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            javaParameters.set(true)
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir("build/generated/source/proto/main/java")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { }
            }
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.animation.graphics.android)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.biometric)
    implementation(libs.compose.m3) // Vico charts
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.lifecycle.viewmodel) // Use the latest version
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.material.material)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.introshowcaseview)
    implementation(libs.tink.android)
    implementation(libs.androidx.datastore.core)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.google.protobuf.java)
    {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }
    implementation(libs.lottie.compose)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.app.update.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}