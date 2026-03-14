rootProject.name = "jframe"

include("jframe-core")

// Spring modules — grouped under jframe-spring/
include("jframe-spring-core")
project(":jframe-spring-core").projectDir = file("jframe-spring/core")

include("jframe-spring-jpa")
project(":jframe-spring-jpa").projectDir = file("jframe-spring/jpa")

include("jframe-spring-otlp")
project(":jframe-spring-otlp").projectDir = file("jframe-spring/otlp")

// Quarkus modules — grouped under jframe-quarkus/
include("jframe-quarkus-core")
project(":jframe-quarkus-core").projectDir = file("jframe-quarkus/core")

include("jframe-quarkus-jpa")
project(":jframe-quarkus-jpa").projectDir = file("jframe-quarkus/jpa")

include("jframe-quarkus-otlp")
project(":jframe-quarkus-otlp").projectDir = file("jframe-quarkus/otlp")

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
