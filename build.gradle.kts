plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.dotenv)
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

val mavenUser = dotEnv["MAVEN_USERNAME"]
val mavenToken = dotEnv["MAVEN_TOKEN"]

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
    // Copied into the task: a doFirst reading these from the script holds a script reference, which the
    // configuration cache cannot serialize.
    val tag = releaseTag
    val tags = headTags
    val dirty = uncommitted
    val hasUser = hasMavenUser
    val hasToken = hasMavenToken

    doFirst {
        require(tag in tags.get().lines()) { "Publishing $tag needs HEAD tagged $tag." }
        require(dirty.get().isBlank()) { "Publishing needs a clean checkout. Commit or stash everything first." }
        require(hasUser) { "MAVEN_USERNAME is not set. Copy .env.example to .env and fill it in." }
        require(hasToken) { "MAVEN_TOKEN is not set. Copy .env.example to .env and fill it in." }
    }
}
