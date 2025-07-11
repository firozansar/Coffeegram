import io.gitlab.arturbosch.detekt.Detekt
import ru.beryukhov.android.withVersionCatalog

plugins {
    /**
     * https://docs.gradle.org/current/userguide/base_plugin.html
     * base plugin added to add wiring on check->build tasks
     */
    base
    id("io.gitlab.arturbosch.detekt")
}

// workaround for https://github.com/gradle/gradle/issues/15383
project.withVersionCatalog { libs ->
    dependencies {
        detektPlugins(libs.detektFormatting)
        detektPlugins(libs.detektCompose)
        detektPlugins(libs.detektDecompose)
    }
}

val detektAll = tasks.register<Detekt>("detektAll") {
    description = "Runs over whole code base without the starting overhead for each module."
    parallel = true
    setSource(files(projectDir))

    /**
     * About config:
     * yaml is a copy of https://github.com/detekt/detekt/blob/master/detekt-core/src/main/resources/default-detekt-config.yml
     * all rules are disabled by default, enabled one by one
     */
    config.setFrom(files(project.rootDir.resolve("detektConfig/detekt.yml")))
    buildUponDefaultConfig = false

    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")
    reports {
        xml.required.set(false)
        html.required.set(false)
    }
}

tasks.named("check").configure {
    dependsOn(detektAll)
}
