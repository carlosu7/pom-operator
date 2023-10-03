package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.KindJ

/**
 * Represents a tuple (kind / version string) applicable from a pom.xml file
 *
 * For Internal Consumption (thus Internal)
 */
internal data class VersionDefinition(
    val kind: KindJ,
    val value: String,
)