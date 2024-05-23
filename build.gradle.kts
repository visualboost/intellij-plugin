import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "visualboost.intellij"
version = "0.0.1"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.3.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("android"))
    updateSinceUntilBuild.set(false)
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
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        val properties = Properties()
        val propertyFile = project.rootProject.file("local.properties")
        if(propertyFile.exists()){
            properties.load(propertyFile.inputStream())
        }

        val VISUALBOOST_RELEASE_TOKEN = properties.getProperty("VISUALBOOST_RELEASE_TOKEN")
        token.set(VISUALBOOST_RELEASE_TOKEN)
    }
}

dependencies{
    implementation("com.google.code.gson:gson:2.10.1")
}