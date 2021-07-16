/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

version = project.property("firebase-app.version") as String

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    google()
    jcenter()
}

android {
    compileSdkVersion(property("targetSdkVersion") as Int)
    defaultConfig {
        minSdkVersion(property("minSdkVersion") as Int)
        targetSdkVersion(property("targetSdkVersion") as Int)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }
    packagingOptions {
        pickFirst("META-INF/kotlinx-serialization-core.kotlin_module")
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
    }
    lintOptions {
        isAbortOnError = false
    }
}

kotlin {

    android {
        publishAllLibraryVariants()
    }

    fun nativeTargetConfig(): KotlinNativeTarget.() -> Unit = {
        val nativeFrameworkPaths = listOf(
            projectDir.resolve("src/nativeInterop/cinterop/Carthage/Build/iOS")
        )

        binaries {
            getTest("DEBUG").apply {
                linkerOpts(nativeFrameworkPaths.map { "-F$it" })
                linkerOpts("-ObjC")
            }
        }

        compilations.getByName("main") {
            cinterops.create("FirebaseCore") {
                compilerOpts(nativeFrameworkPaths.map { "-F$it" })
                extraOpts("-verbose")
            }
        }
    }

    val iosTarget: (String, KotlinNativeTarget.() -> Unit) -> KotlinNativeTarget =
        if (System.getenv("SDK_NAME")?.startsWith("iphoneos") == true)
            ::iosArm64
        else
            ::iosX64

    iosTarget("ios", nativeTargetConfig())

    js {
        useCommonJs()
        nodejs {
            testTask {
                useMocha {
                    timeout = "5s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "5s"
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "1.5"
                languageVersion = "1.5"
                progressiveMode = true
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(project(":firebase-common"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("com.google.firebase:firebase-common:19.5.0")
            }
        }

        val iosMain by getting

        val jsMain by getting
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
