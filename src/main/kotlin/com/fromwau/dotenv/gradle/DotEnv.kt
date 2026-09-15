package com.fromwau.dotenv.gradle

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

/**
 * The `.env` in the root project's directory, as `KEY=VALUE` lines; blank lines and lines starting with `#` are
 * skipped. A variable set in the environment wins over the file, unless it is blank. Both are read through Gradle's
 * providers, so the configuration cache notices when either one changes.
 */
public class DotEnv internal constructor(
    private val providers: ProviderFactory,
    text: Provider<String>,
) {
    private val values: Provider<Map<String, String>> = text.map(::parseDotEnv)

    /** The value of [name], or null when neither the environment nor `.env` sets it. */
    public operator fun get(name: String): String? = provider(name).orNull

    /** The value of [name] as a provider, read only when a task or setting asks for it. */
    public fun provider(name: String): Provider<String> = providers
        .environmentVariable(name)
        .map { value -> value.takeIf { it.isNotBlank() } }
        .orElse(values.map { it[name] })
}

private fun parseDotEnv(text: String): Map<String, String> = text
    .lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
    .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
