package com.fromwau.dotenv.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains

/** Real builds with the plugin, for what only a whole build shows: the environment and the configuration cache. */
class DotEnvFunctionalTest {
    private val dir: File = createTempDirectory("dotenv-gradle").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun project(dotEnv: String) {
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"probe\"\n")
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.fromwau.dotenv") }

            tasks.register("printValue") {
                val value = dotEnv["DOTENV_TEST"]
                doLast { println("value=" + value) }
            }
            """.trimIndent(),
        )
        dir.resolve(".env").writeText(dotEnv)
    }

    private fun build(
        vararg arguments: String,
        environment: Map<String, String> = emptyMap(),
    ): String = GradleRunner
        .create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .withEnvironment(System.getenv() + environment)
        .withArguments(*arguments)
        .build()
        .output

    @Test
    fun `an environment variable wins over the file`() {
        project("DOTENV_TEST=from-file\n")

        assertContains(build("printValue", environment = mapOf("DOTENV_TEST" to "from-env")), "value=from-env")
    }

    @Test
    fun `a blank environment variable counts as unset`() {
        project("DOTENV_TEST=from-file\n")

        assertContains(build("printValue", environment = mapOf("DOTENV_TEST" to " ")), "value=from-file")
    }

    @Test
    fun `an edited env file invalidates the configuration cache`() {
        project("DOTENV_TEST=one\n")
        build("printValue", "--configuration-cache")
        assertContains(build("printValue", "--configuration-cache"), "Reusing configuration cache.")

        dir.resolve(".env").writeText("DOTENV_TEST=two\n")
        val output = build("printValue", "--configuration-cache")

        assertContains(output, "file '.env' has changed")
        assertContains(output, "value=two")
    }
}
