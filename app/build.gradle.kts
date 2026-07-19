import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("com.google.protobuf") version "0.10.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

// App release version is intentionally manual. Update these values for each release.
val appVersionName = "3.0.0"
val appVersionCode = 3_000_000

fun releaseSecret(name: String): String? =
    providers.gradleProperty(name).orNull?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.environmentVariable(name).orNull?.trim()?.takeIf { it.isNotEmpty() }

val releaseStorePath = releaseSecret("UPITRACKER_STORE_FILE")
    ?: (project.findProperty("android.injected.signing.store.file") as? String)
    ?: System.getProperty("android.injected.signing.store.file")
val releaseStorePassword = releaseSecret("UPITRACKER_STORE_PASSWORD")
    ?: (project.findProperty("android.injected.signing.store.password") as? String)
    ?: System.getProperty("android.injected.signing.store.password")
val releaseKeyAlias = releaseSecret("UPITRACKER_KEY_ALIAS")
    ?: (project.findProperty("android.injected.signing.key.alias") as? String)
    ?: System.getProperty("android.injected.signing.key.alias")
val releaseKeyPassword = releaseSecret("UPITRACKER_KEY_PASSWORD")
    ?: (project.findProperty("android.injected.signing.key.password") as? String)
    ?: System.getProperty("android.injected.signing.key.password")
val releaseStoreFile = releaseStorePath?.let { file(it) }
val hasReleaseSigning = releaseStoreFile?.isFile == true &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null
val releaseSigningRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true) &&
        (it.contains("assemble", ignoreCase = true) ||
            it.contains("bundle", ignoreCase = true) ||
            it.contains("package", ignoreCase = true) ||
            it.contains("install", ignoreCase = true))
}
if (releaseSigningRequested && !hasReleaseSigning) {
    throw GradleException(
        "Release signing is incomplete. Check UPITRACKER_STORE_FILE, UPITRACKER_STORE_PASSWORD, " +
            "UPITRACKER_KEY_ALIAS, and UPITRACKER_KEY_PASSWORD in ~/.gradle/gradle.properties. " +
            "Refusing to create a release artifact with a debug key."
    )
}

android {
    namespace = "com.example.upitracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.upitracker"
        minSdk = 33
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                this.storeFile = releaseStoreFile
                this.storePassword = releaseStorePassword
                this.keyAlias = releaseKeyAlias
                this.keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
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
