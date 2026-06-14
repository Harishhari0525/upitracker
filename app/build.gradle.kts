import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.net.HttpURLConnection
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("com.google.protobuf") version "0.10.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
}

fun getLatestGithubVersion(): String {
    try {
        val connection = URI.create("https://api.github.com/repos/Harishhari0525/upitracker/releases/latest").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val tagMatch = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(response)
            if (tagMatch.find()) {
                val tag = tagMatch.group(1).removePrefix("v")
                val parts = tag.split(".")
                val lastPart = parts.last().toIntOrNull()
                if (lastPart != null) {
                    val incrementedLast = lastPart + 1
                    val newParts = parts.dropLast(1) + incrementedLast.toString()
                    return newParts.joinToString(".")
                }
                return tag
            }
        }
    } catch (_: Exception) {
        // Fallback silently if offline or rate-limited
    }
    return "2.0.28"
}

android {
    namespace = "com.example.upitracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.upitracker"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = (project.findProperty("versionName") as? String) ?: getLatestGithubVersion()

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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
                create("java") { }
            }
        }
    }
}


dependencies {
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
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