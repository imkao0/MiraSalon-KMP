@file:Suppress("UnstableApiUsage")
rootProject.name = "MiraSalon-KMP"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
}

val skipAndroid = System.getProperty("skipAndroid") == "true"

if (!skipAndroid) {
    include(":androidApp")
}
include(":composeApp")
include(":desktopApp")
include(":server")

// Core modules
include(":core:common")
include(":core:database")
include(":core:designsystem")
include(":core:domain")
include(":core:navigation")
include(":core:network")
include(":core:realtime")
include(":core:testing")

// Feature modules
include(":feature:feature-appointments")
include(":feature:feature-auth")
include(":feature:feature-booking")
include(":feature:feature-cart")
include(":feature:feature-chat")
include(":feature:feature-notifications")
include(":feature:feature-profile")
include(":feature:feature-services-discovery")
include(":feature:feature-salon")
include(":feature:feature-products")
include(":feature:feature-specialists")
include(":feature:feature-favourites")
