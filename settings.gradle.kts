pluginManagement {
    val springBootPluginVersion = providers.gradleProperty("springBootPluginVersion").get()
    val springDependencyPluginVersion = providers.gradleProperty("springDependencyPluginVersion").get()
    val qualityPluginVersion = providers.gradleProperty("qualityPluginVersion").get()
    val cycloneDxPluginVersion = providers.gradleProperty("cycloneDxPluginVersion").get()
    val spotbugsPluginVersion = providers.gradleProperty("spotbugsPluginVersion").get()
    val lombokPluginVersion = providers.gradleProperty("lombokPluginVersion").get()
    val spotlessPluginVersion = providers.gradleProperty("spotlessPluginVersion").get()
    val dependencyUpdatesPluginVersion = providers.gradleProperty("dependencyUpdatesPluginVersion").get()
    val publishingVersion = providers.gradleProperty("publishingVersion").get()
    val nmcpPluginVersion = providers.gradleProperty("nmcpPluginVersion").get()
    val jandexPluginVersion = providers.gradleProperty("jandexPluginVersion").get()

    plugins {
        id("org.springframework.boot") version springBootPluginVersion
        id("io.spring.dependency-management") version springDependencyPluginVersion
        id("ru.vyarus.quality") version qualityPluginVersion
        id("org.cyclonedx.bom") version cycloneDxPluginVersion
        id("com.github.spotbugs") version spotbugsPluginVersion
        id("io.freefair.lombok") version lombokPluginVersion
        id("com.diffplug.spotless") version spotlessPluginVersion
        id("io.github.ben-manes.versions") version dependencyUpdatesPluginVersion
        id("publishing") version publishingVersion
        id("com.gradleup.nmcp.settings") version nmcpPluginVersion
        id("com.github.vlsi.jandex") version jandexPluginVersion
    }
}

plugins {
    id("com.gradleup.nmcp.settings")
}

nmcpSettings {
    centralPortal {
        username = System.getenv("MAVEN_USERNAME") ?: ""
        password = System.getenv("MAVEN_PASSWORD") ?: ""
        publishingType = "AUTOMATIC"
    }
}

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

include("jframe-quarkus-otlp-deployment")
project(":jframe-quarkus-otlp-deployment").projectDir = file("jframe-quarkus/otlp-deployment")
