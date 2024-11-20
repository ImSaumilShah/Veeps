@file:Suppress("UnstableApiUsage")

pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
		gradlePluginPortal()
		maven { url = uri("https://www.jitpack.io") }
		maven { url = uri("https://artifacts.bitmovin.com/artifactory/public-releases") }
	}
}
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
		maven { url = uri("https://www.jitpack.io") }
		maven { url = uri("https://artifacts.bitmovin.com/artifactory/public-releases") }
	}
	versionCatalogs {

	}
}

rootProject.name = "Veeps"
include(":app")
