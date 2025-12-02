rootProject.name = "jframe"
val modules = arrayOf(
    "starter-core",
    "starter-otlp",
    "starter-jpa"
)
modules.forEach { name -> include(name)}

pluginManagement {
    val springBootPluginVersion: String by settings
    val springDependencyPluginVersion: String by settings
    val qualityPluginVersion: String by settings
    val cycloneDxPluginVersion: String by settings
    val spotbugsPluginVersion: String by settings
    val lombokPluginVersion: String by settings
    val spotlessPluginVersion: String by settings
    val dependencyUpdatesPluginVersion: String by settings
    val publishingVersion: String by settings
    val nmcpPluginVersion: String by settings

    plugins {
        id("org.springframework.boot") version springBootPluginVersion
        id("io.spring.dependency-management") version springDependencyPluginVersion
        id("ru.vyarus.quality") version qualityPluginVersion
        id("org.cyclonedx.bom") version cycloneDxPluginVersion
        id("com.github.spotbugs") version spotbugsPluginVersion
        id("io.freefair.lombok") version lombokPluginVersion
        id("com.diffplug.spotless") version spotlessPluginVersion
        id("com.github.ben-manes.versions") version dependencyUpdatesPluginVersion
        id("publishing") version publishingVersion
        id("com.gradleup.nmcp.aggregation") version nmcpPluginVersion
    }
}
