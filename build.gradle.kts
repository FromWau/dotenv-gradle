plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.fromwau"
version = libs.versions.dotenvGradleVersion.get()
description = "A Gradle plugin that reads a project's .env into its build scripts, with the environment winning."

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.jdk.get().toInt())
}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("dotenv") {
            id = "com.fromwau.dotenv"
            implementationClass = "com.fromwau.dotenv.gradle.DotEnvPlugin"
            displayName = "dotenv-gradle"
            description = project.description
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    into("META-INF") {
        from(layout.projectDirectory.file("LICENSE")) { rename { "LICENSE-dotenv-gradle.txt" } }
    }
}

// This build cannot apply the plugin it builds, so it reads its publishing credentials the same way by hand: the
// .env beside it, with a variable set in the environment winning unless it is blank.
val dotEnv: Map<String, String> = providers
    .fileContents(layout.projectDirectory.file(".env"))
    .asText
    .orNull
    .orEmpty()
    .lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
    .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }

fun credential(name: String): String? =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() } ?: dotEnv[name]

val mavenUser = credential("MAVEN_USERNAME")
val mavenToken = credential("MAVEN_TOKEN")

val repoSlug = "FromWau/dotenv-gradle"
val repoUrl = "https://github.com/$repoSlug"

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "dotenv-gradle"
            description = project.description
            url = repoUrl
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    distribution = "repo"
                }
            }
            scm {
                url = repoUrl
                connection = "scm:git:$repoUrl.git"
                developerConnection = "scm:git:ssh://git@github.com/$repoSlug.git"
            }
        }
    }

    repositories {
        maven {
            name = "vps"
            url = uri("https://maven.frommhund.xyz/releases")
            credentials {
                username = mavenUser.orEmpty()
                password = mavenToken.orEmpty()
            }
            authentication { create<BasicAuthentication>("basic") }
        }
    }
}

val hasMavenUser = !mavenUser.isNullOrBlank()
val hasMavenToken = !mavenToken.isNullOrBlank()

// A version names exactly one commit: publishing needs a clean checkout whose HEAD carries the tag v<version>.
val releaseTag = "v$version"
val headTags = providers.exec {
    commandLine("git", "tag", "--points-at", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText
val uncommitted = providers.exec {
    commandLine("git", "status", "--porcelain")
    isIgnoreExitValue = true
}.standardOutput.asText

tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst {
        require(releaseTag in headTags.get().lines()) { "Publishing $releaseTag needs HEAD tagged $releaseTag." }
        require(uncommitted.get().isBlank()) { "Publishing needs a clean checkout. Commit or stash everything first." }
        require(hasMavenUser) { "MAVEN_USERNAME is not set. Copy .env.example to .env and fill it in." }
        require(hasMavenToken) { "MAVEN_TOKEN is not set. Copy .env.example to .env and fill it in." }
    }
}
