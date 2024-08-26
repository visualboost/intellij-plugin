import java.net.URI
import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
//    id("org.jetbrains.intellij") version "1.17.4"

    id("org.jetbrains.intellij.platform") version "2.0.0"
    id("org.jetbrains.intellij.platform.migration") version "2.0.0"
}

group = "visualboost.intellij"
version = "1.0.0-alpha.01"

repositories {
    mavenCentral()

    val properties = Properties()
    val propertiesFile = project.rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }

    val MVN_URL_REPO = System.getenv("MVN_URL_BASE_REPO") ?: properties.getProperty("MVN_URL_BASE_REPO")
    val MVN_TOKEN = System.getenv("MVN_TOKEN") ?: properties.getProperty("MVN_TOKEN")

    maven {
        name = "bytesafe"
        url = URI.create(MVN_URL_REPO)
        credentials {
            username = "bytesafe"
            password = MVN_TOKEN
        }
    }

    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
//intellij {
//    version.set("2022.3.3")
//    type.set("IC") // Target IDE Platform
//
//    plugins.set(listOf("Git4Idea", "java"))
//    updateSinceUntilBuild.set(false)
//}

intellijPlatform {
    pluginConfiguration {
//        version.set("2022.3.3")
        name.set("VisualBoost")
    }

}


/**
 * Stop caching dependencies
 */
configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(0, "seconds")
        cacheChangingModulesFor(0, "seconds")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        val properties = Properties()
        val propertyFile = project.rootProject.file("local.properties")
        if (propertyFile.exists()) {
            properties.load(propertyFile.inputStream())
        }

        val VISUALBOOST_RELEASE_TOKEN = properties.getProperty("VISUALBOOST_RELEASE_TOKEN")
        token.set(VISUALBOOST_RELEASE_TOKEN)
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-api:1.7.25")

//    implementation("visualboost:codegen_base-lib:0.1.46")

    intellijPlatform {
        jetbrainsRuntime()

//        intellijIdeaCommunity("2024.1.2")
//        bundledPlugins("Git4Idea")

        webstorm("2024.1.2")
        bundledPlugins("Git4Idea", "JavaScript", "Docker")

        pluginVerifier()
        zipSigner()
        instrumentationTools()

    }
}