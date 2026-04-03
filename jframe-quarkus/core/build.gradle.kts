description = "JFrame Quarkus Core - JAX-RS filters, exception mappers, and CDI utilities for Quarkus"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-core"))

    // Quarkus APIs — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly("jakarta.validation", "jakarta.validation-api", retrieve("jakartaValidationVersion"))
    compileOnly("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    compileOnly("org.slf4j", "slf4j-api", retrieve("slf4jVersion"))
    compileOnly("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))
    compileOnly("jakarta.enterprise", "jakarta.enterprise.cdi-api", retrieve("jakartaCdiVersion"))
    compileOnly("io.smallrye", "smallrye-open-api-core", retrieve("smallryeOpenApiVersion"))
    compileOnly("io.quarkus", "quarkus-smallrye-openapi", retrieve("quarkusVersion"))
    compileOnly("io.quarkus", "quarkus-arc", retrieve("quarkusVersion"))
    compileOnly("io.quarkus", "quarkus-core", retrieve("quarkusVersion"))

    // Test dependencies
    testImplementation("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    testImplementation("jakarta.validation", "jakarta.validation-api", retrieve("jakartaValidationVersion"))
    testImplementation("jakarta.enterprise", "jakarta.enterprise.cdi-api", retrieve("jakartaCdiVersion"))
    testImplementation("io.quarkus", "quarkus-arc", retrieve("quarkusVersion"))
    testImplementation("io.quarkus", "quarkus-core", retrieve("quarkusVersion"))
    testImplementation("org.junit.jupiter", "junit-jupiter", retrieve("junitVersion"))
    testImplementation("org.mockito", "mockito-core", retrieve("mockitoVersion"))
    testImplementation("org.mockito", "mockito-junit-jupiter", retrieve("mockitoVersion"))
    testImplementation("ch.qos.logback", "logback-classic", retrieve("logbackVersion"))
    testRuntimeOnly("org.jboss.resteasy", "resteasy-core", retrieve("resteasyCoreVersion"))
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", retrieve("junitVersion"))
}
