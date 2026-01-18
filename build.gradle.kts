// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**")
            ktlint("1.5.0")
                .editorConfigOverride(
                    mapOf(
                        "ktlint_standard_package-name" to "disabled",
                        "ktlint_standard_function-naming" to "disabled",
                        "ktlint_standard_no-wildcard-imports" to "disabled",
                        "ktlint_standard_property-naming" to "disabled",
                        "ktlint_standard_value-parameter-comment" to "disabled",
                        "ktlint_standard_backing-property-naming" to "disabled"
                    )
                )
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint("1.5.0")
        }
    }
}