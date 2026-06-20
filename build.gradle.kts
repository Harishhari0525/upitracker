// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.devtools.ksp") version "2.3.9"
}

// Root build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.symbol.processing.gradle.plugin)
    }
}

subprojects {
    configurations.all {
        resolutionStrategy {
            force("org.jdom:jdom2:2.0.6.1")
            force("org.bitbucket.b_c:jose4j:0.9.6")
            force("io.netty:netty-codec-http2:4.1.135.Final")
            force("io.netty:netty-handler:4.1.135.Final")
            force("io.netty:netty-codec-http:4.1.135.Final")
            force("io.netty:netty-codec:4.1.135.Final")
        }
    }
}