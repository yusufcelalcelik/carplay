import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

include(":app", ":contract", ":carplay-engine", ":carplay-lab")


rootProject.name = "open-headunit"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://jitpack.io")
        }
    }
}
