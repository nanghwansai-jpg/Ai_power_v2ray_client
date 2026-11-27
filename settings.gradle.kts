pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Maven Central mirror for CI resilience when repo.maven.apache.org is rate-limited.
        maven {
            name = "MavenCentralMirror"
            url = uri("https://repo1.maven.org/maven2")
            content {
                includeModule("junit", "junit")
                includeModule("org.hamcrest", "hamcrest-core")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "AiPowerV2RayClient"
include(":android:app")
