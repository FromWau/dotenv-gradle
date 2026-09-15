package com.fromwau.dotenv.gradle

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// The keys are ones no real environment sets, since a variable in the environment wins over the file.
class DotEnvPluginTest {
    private val dir: File = createTempDirectory("dotenv-gradle").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun dotEnv(content: String? = null): DotEnv {
        if (content != null) dir.resolve(".env").writeText(content)
        val project = ProjectBuilder
            .builder()
            .withProjectDir(dir)
            .build()
        project.pluginManager.apply("com.fromwau.dotenv")
        return project.extensions.getByType(DotEnv::class.java)
    }

    @Test
    fun `reads KEY=VALUE lines`() {
        val env = dotEnv("DOTENV_TEST_A=one\nDOTENV_TEST_B=two\n")

        assertEquals("one", env["DOTENV_TEST_A"])
        assertEquals("two", env["DOTENV_TEST_B"])
    }

    @Test
    fun `skips blank lines and comments and lines without an equals sign`() {
        val env = dotEnv("# credentials\n\nnot a pair\n  DOTENV_TEST_A = a=b  \n")

        assertEquals("a=b", env["DOTENV_TEST_A"])
    }

    @Test
    fun `an empty value is an empty string`() {
        assertEquals("", dotEnv("DOTENV_TEST_A=\n")["DOTENV_TEST_A"])
    }

    @Test
    fun `a repeated key takes its last value`() {
        assertEquals("two", dotEnv("DOTENV_TEST_A=one\nDOTENV_TEST_A=two\n")["DOTENV_TEST_A"])
    }

    @Test
    fun `a key the file does not set is null`() {
        assertNull(dotEnv("DOTENV_TEST_A=value\n")["DOTENV_TEST_B"])
    }

    @Test
    fun `a missing file sets nothing`() {
        assertNull(dotEnv()["DOTENV_TEST_A"])
    }

    @Test
    fun `a subproject reads the root project's file`() {
        dir.resolve(".env").writeText("DOTENV_TEST_A=from-root\n")
        val root = ProjectBuilder
            .builder()
            .withProjectDir(dir)
            .build()
        val child = ProjectBuilder
            .builder()
            .withParent(root)
            .withName("child")
            .build()
        child.pluginManager.apply("com.fromwau.dotenv")

        assertEquals("from-root", child.extensions.getByType(DotEnv::class.java)["DOTENV_TEST_A"])
    }

    @Test
    fun `provider reads the file's value`() {
        assertEquals("one", dotEnv("DOTENV_TEST_A=one\n").provider("DOTENV_TEST_A").get())
    }
}
