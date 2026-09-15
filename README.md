# dotenv-gradle

A Gradle plugin that reads a project's `.env` into its build scripts at build time, for publishing credentials,
keys and tokens that must never be committed.

## Add to your build

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        maven("https://maven.frommhund.xyz/releases")
        gradlePluginPortal()
    }
}

// build.gradle.kts
plugins {
    id("com.fromwau.dotenv") version "<version>"
}
```

To apply it from a convention plugin instead, add the artifact to `build-logic/build.gradle.kts` and apply
`id("com.fromwau.dotenv")` there without a version. List the repository in both `build-logic`'s settings and the
consuming build's `pluginManagement`, since every project that applies the convention loads the plugin from there.

```kotlin
dependencies {
    implementation("com.fromwau:dotenv-gradle:<version>")
}
```

Published versions are listed at
[maven.frommhund.xyz](https://maven.frommhund.xyz/#/releases/com/fromwau/dotenv-gradle). The plugin is Java 21
bytecode, so Gradle's daemon must run on JDK 21 or newer. It is built and tested with Gradle 9 only.

## Reading values

```kotlin
val token: String? = dotEnv["MAVEN_TOKEN"]                     // the value now, or null
val later: Provider<String> = dotEnv.provider("MAVEN_TOKEN")    // read when a task or setting asks
```

- **The file** is the `.env` in the root project's directory, the same one for every project in the build.
  Keep it out of git, and commit a `.env.example` that lists the keys.
- **The format** is one `KEY=VALUE` per line. Blank lines, lines starting with `#` and lines without `=` are
  skipped, and spaces around the key and the value are trimmed. Everything after the first `=` is the value, taken
  as written: there are no quotes, no `export` and no `${}` expansion. `KEY=` gives an empty string, and a
  repeated key takes its last value.
- **The environment wins.** A variable set in the environment replaces the file's value, so CI can set values
  without a file. A blank variable counts as unset.
- **The configuration cache** notices changes. Both the file and the environment are read through Gradle's
  providers, so editing `.env` makes the next build reconfigure instead of reusing a stale value.

## License

[Apache-2.0](LICENSE).
