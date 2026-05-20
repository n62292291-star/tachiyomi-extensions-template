plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("kotlin")
    id("com.android.application")
}

group = "eu.kanade.tachiyomi.extension.zh"
version = "1.0"

dependencies {
    implementation(project(":lib-multisrc"))
    implementation(project(":core"))
}
