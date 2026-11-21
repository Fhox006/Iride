import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val baseApplicationId = "com.iride.music"
val applicationIdOverride = System.getenv("METROLIST_APPLICATION_ID")?.takeIf { it.isNotBlank() }
val appNameOverride = System.getenv("METROLIST_APP_NAME")?.takeIf { it.isNotBlank() }
val debugKeystorePathOverride = System.getenv("METROLIST_DEBUG_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val debugKeystorePassword = System.getenv("METROLIST_DEBUG_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
val debugKeyAlias = System.getenv("METROLIST_DEBUG_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "androiddebugkey"
val debugKeyPassword = System.getenv("METROLIST_DEBUG_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "android"
val persistentDebugKeystoreFile = file("persistent-debug.keystore")
val workflowDebugKeystoreFile = debugKeystorePathOverride?.let(::file)

plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.metrolist.music"
    compileSdk = 36

    defaultConfig {
        applicationId = applicationIdOverride ?: baseApplicationId
        minSdk = 26
        targetSdk = 36
        versionCode = 36
        versionName = "0.1.0-alpha36"
        resValue("string", "app_name", appNameOverride ?: "Iride")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: ""
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: ""
        if (lastFmKey.isBlank()) {
            logger.warn("WARNING: LASTFM_API_KEY is not set. LastFM login will be disabled at runtime.")
        }

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")
        buildConfigField("String", "ARCHITECTURE", "\"universal\"")
    }

    flavorDimensions += listOf("variant")
    productFlavors {
        create("foss") {
            dimension = "variant"
            isDefault = true
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        create("gms") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "true")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "true")
        }

        create("izzy") {
            dimension = "variant"
            buildConfigField("Boolean", "CAST_AVAILABLE", "false")
            buildConfigField("Boolean", "UPDATER_AVAILABLE", "false")
        }
    }

    signingConfigs {
        create("persistentDebug") {
            storeFile = persistentDebugKeystoreFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("workflowDebug") {
            storeFile = workflowDebugKeystoreFile ?: persistentDebugKeystoreFile
            storePassword = debugKeystorePassword
            keyAlias = debugKeyAlias
            keyPassword = debugKeyPassword
        }
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (workflowDebugKeystoreFile != null) {
                    signingConfigs.getByName("workflowDebug")
                } else if (persistentDebugKeystoreFile.exists()) {
                    signingConfigs.getByName("persistentDebug")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
        debug {
            if (applicationIdOverride == null) {
                applicationIdSuffix = ".debug"
            }
            isDebuggable = true
            if (appNameOverride == null) {
                resValue("string", "app_name", "Iride Debug")
            }
            signingConfig =
                if (workflowDebugKeystoreFile != null) {
                    signingConfigs.getByName("workflowDebug")
                } else if (persistentDebugKeystoreFile.exists()) {
                    signingConfigs.getByName("persistentDebug")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
        create("performance") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            matchingFallbacks += listOf("debug")
            if (applicationIdOverride == null) {
                applicationIdSuffix = ".performance"
            }
            if (appNameOverride == null) {
                resValue("string", "app_name", "Iride Test")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    sourceSets.named("main") {
        kotlin.srcDir("src/main/rendering")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
        warningsAsErrors = false
        abortOnError = false
        checkDependencies = false
    }

    androidResources {
        generateLocaleConfig = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols +=
                listOf(
                    "**/libandroidx.graphics.path.so",
                    "**/libdatastore_shared_counter.so",
                )
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "com/atilika/kuromoji/ipadic/**"
        }
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("performance")) { variant ->
        providers.gradleProperty("iride.performanceMinSdk").orNull?.let { value ->
            val minimumSdk = value.toInt()
            require(minimumSdk in 26..36) { "iride.performanceMinSdk must be between 26 and 36" }
            variant.minSdk = minimumSdk
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    // Keep rendering-only edits out of Room/Hilt inputs; processor-visible types stay
    // in the regular Kotlin source root. KSP excludes source roots, not individual files.
    excludedSources.from(file("src/main/rendering"))
}

val validateRenderingAnnotations = tasks.register("validateRenderingAnnotations") {
    val renderingSources = fileTree("src/main/rendering") { include("**/*.kt") }
    inputs.files(renderingSources).withPathSensitivity(PathSensitivity.RELATIVE)
    doLast {
        val allowed = setOf(
            "Composable", "OptIn", "Suppress", "SuppressLint", "Preview", "Stable", "Immutable",
            "ReadOnlyComposable", "NonRestartableComposable", "NonSkippableComposable",
        )
        val annotation = Regex("""(?<!\w)@(?:\w+\.)*(\w+)""")
        renderingSources.forEach { source ->
            val text = source.readText()
            val unsupported = annotation.findAll(text).firstOrNull { it.groupValues[1] !in allowed }
            check(unsupported == null && !Regex("""(?<!\w)@\[""").containsMatchIn(text)) {
                "${source.name}: move processor annotations into src/main/kotlin; rendering sources bypass KSP."
            }
        }
    }
}

tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }.configureEach {
    dependsOn(validateRenderingAnnotations)
}

// KSP 2.3.12's AGP 9 integration re-adds Kotlin roots as Java inputs without
// honoring excludedSources. Filter the final roots after that integration runs.
afterEvaluate {
    tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
        val renderingRoot = layout.projectDirectory.dir("src/main/rendering").asFile.toPath()
        val processingRoots = files(kspConfig.javaSourceRoots.from.toList())
        kspConfig.javaSourceRoots.setFrom(processingRoots.filter { !it.toPath().startsWith(renderingRoot) })
    }
}

// Archiving the app's whole compiler output costs more than compiling a local edit.
// Preserve incremental state locally; CI and explicit cache restores retain the cache.
val cacheAppCompilation = providers.gradleProperty("iride.cacheAppCompilation")
    .orElse(providers.environmentVariable("CI"))
    .map(String::toBoolean)
    .getOrElse(false)
tasks.matching {
    it.name.startsWith("compile") &&
        (it.name.endsWith("DebugKotlin") || it.name.endsWith("PerformanceKotlin"))
}.configureEach {
    if (!cacheAppCompilation) {
        outputs.doNotCacheIf("Local app compilation keeps incremental outputs in place") { true }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
        suppressWarnings.set(false)
    }
}

// Keep the workspace's compile-only verification command meaningful for the flavored app.
// Without this alias, `compileDebugKotlin` selects only library modules and skips all app UI.
tasks.register("compileDebugKotlin") {
    dependsOn("compileFossDebugKotlin")
}

configurations.configureEach {
    exclude(group = "org.json", module = "json")
}

dependencies {
    implementation("net.java.dev.jna:jna:5.19.1@aar")
    implementation("org.apache.commons:commons-compress:1.27.1")
    testImplementation(libs.junit)

    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.core.splashscreen)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    // Enable Compose inspection tooling only when needed: -Piride.composeTooling=true.
    if (providers.gradleProperty("iride.composeTooling").orNull.toBoolean()) {
        debugImplementation(libs.compose.ui.tooling)
    }
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.materialKolor)

    implementation(libs.appcompat)

    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    implementation(libs.ucrop)

    implementation(libs.shimmer)
    implementation("io.github.stoyan-vuchev:squircle-shape-android:3.0.1")

    implementation(libs.media3)
    implementation(libs.media3.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    "gmsImplementation"(libs.media3.cast)
    "gmsImplementation"(libs.mediarouter)
    "gmsImplementation"(libs.cast.framework)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation(libs.jsoup)
    implementation(libs.security.crypto)
    ksp(libs.hilt.compiler)

    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":kizzy"))
    implementation(project(":lastfm"))
    implementation(project(":betterlyrics"))
    implementation(project(":shazamkit"))
    implementation(project(":paxsenix"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)
}
