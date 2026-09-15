package com.fromwau.dotenv.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Adds the [DotEnv] extension, named `dotEnv`, which reads the `.env` in the root project's directory. */
public class DotEnvPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val file =
            project.objects.fileProperty().fileValue(project.rootDir.resolve(".env"))
        project.extensions.add(
            "dotEnv",
            DotEnv(project.providers, project.providers.fileContents(file).asText)
        )
    }
}
