pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            // Mapbox requires authentication - read from local.properties
            val localProperties = java.util.Properties()
            val localPropertiesFile = File(rootDir, "local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { localProperties.load(it) }
            }
            credentials.username = "mapbox"
            credentials.password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                ?: localProperties.getProperty("MAPBOX_ACCESS_TOKEN")
                ?: ""
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "petsitternow_app"
include(":app")
 