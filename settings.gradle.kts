@file:Suppress("UnstableApiUsage")

val fastIdeBuild = providers.gradleProperty("android.injected.invoked.from.ide")
    .map(String::toBoolean).getOrElse(false) &&
    providers.gradleProperty("iride.fastIdeBuild").map(String::toBoolean).getOrElse(true)
if (fastIdeBuild) {
    buildCache {
        local { isEnabled = false }
    }
    // Model-only sync does not register lint tasks, so exclusions cannot resolve there.
    if (gradle.startParameter.taskNames.isNotEmpty()) {
        gradle.startParameter.setExcludedTaskNames(
            gradle.startParameter.excludedTaskNames + listOf(
                ":app:lintVitalFossPerformance",
                ":app:lintVitalGmsPerformance",
                ":app:lintVitalIzzyPerformance",
            ),
        )
    }
    logger.lifecycle("Iride: fast Android Studio build enabled (incremental outputs, performance lint excluded).")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}


rootProject.name = "Iride"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":lastfm")
include(":betterlyrics")
include(":shazamkit")
include(":paxsenix")

