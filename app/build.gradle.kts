import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("com.google.protobuf") version "0.10.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
}

val resolvedVersionName = (project.findProperty("versionName") as? String) ?: "2.0.30"
val semanticParts = resolvedVersionName.split('.').map { it.toIntOrNull() ?: 0 }
val calculatedVersionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull()
    ?: ((semanticParts.getOrElse(0) { 0 } * 1_000_000) +
        (semanticParts.getOrElse(1) { 0 } * 1_000) +
        semanticParts.getOrElse(2) { 0 })

android {
    namespace = "com.example.upitracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.upitracker"
        minSdk = 33
        targetSdk = 37
        versionCode = calculatedVersionCode
        versionName = resolvedVersionName

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
            signingConfig = if (signingConfigs.getByName("release").storeFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/*.kotlin_module"
        }
        dex {
            useLegacyPackaging = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        javaParameters.set(true)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") { option("lite") }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.animation.graphics.android)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.introshowcaseview)
    implementation(libs.tink.android)
    implementation(libs.androidx.datastore.core)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.google.protobuf.javalite)
    implementation(libs.lottie.compose)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.coil.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}