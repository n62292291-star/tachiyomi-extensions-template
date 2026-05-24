pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    versionCatalogs {
        create("libs") {
            from(files(rootProject.projectDir.resolve("libs.versions.toml")))
        }
        create("tachiyomi") {
            from(files(rootProject.projectDir.resolve("tachiyomi.versions.toml")))
        }
    }

    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven(url = "https://jitpack.io")
    }
}

plugins {
    id("de.fayard.refreshVersions") version "+"
    id("org.gradle.toolchains.foojay-resolver-convention") version "+"
}

rootProject.apply {
    name = "tachiyomi-extensions-template"
    buildFileName = "env.build.gradle.kts"
}

rootProject.projectDir.resolve("build-src").apply {
    includeBuild(resolve("conventions"))
    includeBuild(resolve("github-api"))
}

includeAllSubprojectsIn(rootProject.projectDir.resolve("utils"), null)

// 注释掉不需要的 cryptoaes 模块（解决依赖拉取失败）
include(":lib-multisrc")
// include(":lib-cryptoaes")

includeAllSubprojectsInRecursively(rootProject.projectDir.resolve("extensions"), "extensions")

fun includeAllSubprojectsIn(dir: File, prefix: String?, expectedScriptName: String? = "build.gradle") {
    if (!dir.exists() || !dir.isDirectory) return

    (dir.listFiles() ?: emptyArray())
        .asSequence()
        .filter { it.isDirectory }
        .filter { d ->
            expectedScriptName == null ||
                d.listFiles()?.any { it.name == expectedScriptName || it.name == "${expectedScriptName}.kts" } == true
        }
        .forEach { inclusion ->
            val path = when (prefix) {
                null -> ":${inclusion.name}"
                else -> ":$prefix-${inclusion.name}" // 修正：规范的 Kotlin 字符串模板写法
            }
            include(path)
            project(path).apply {
                this.projectDir = inclusion
            }
        }
}

fun includeAllSubprojectsInRecursively(root: File, prefix: String?, expectedScriptName: String? = "build.gradle") {
    if (!root.exists() || !root.isDirectory) return

    fileTree(root).forEach { element ->
        val include = element.isDirectory && (expectedScriptName == null ||
            (element.listFiles() ?: emptyArray())
                .map { it.name }
                .let { it.contains(expectedScriptName) || it.contains("${expectedScriptName}.kts") })

        if (include) {
            val path = when (prefix) {
                null -> ":${element.name}"
                else -> ":$prefix-${element.name}" // 修正：规范的 Kotlin 字符串模板写法
            }
            include(path)
            project(path).apply {
                this.projectDir = element
            }
        }
    }
}
