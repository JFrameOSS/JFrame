description = "JFrame Quarkus Core - JAX-RS filters, exception mappers, and CDI utilities for Quarkus"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-core"))

    // Quarkus APIs — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly("jakarta.ws.rs", "jakarta.ws.rs-api", retrieve("jakartaWsrsVersion"))
    compileOnly("org.slf4j", "slf4j-api", retrieve("slf4jVersion"))
    compileOnly("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))

    // Test dependencies
    testImplementation("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    testImplementation("jakarta.ws.rs", "jakarta.ws.rs-api", retrieve("jakartaWsrsVersion"))
    testImplementation("org.junit.jupiter", "junit-jupiter", retrieve("junitVersion"))
    testImplementation("org.mockito", "mockito-core", retrieve("mockitoVersion"))
    testImplementation("org.mockito", "mockito-junit-jupiter", retrieve("mockitoVersion"))
    testImplementation("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))
    testImplementation("org.slf4j", "slf4j-api", retrieve("slf4jVersion"))
    testImplementation("ch.qos.logback", "logback-classic", retrieve("logbackVersion"))
    testImplementation("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))
    testRuntimeOnly("org.jboss.resteasy", "resteasy-core", retrieve("resteasyCoreVersion"))
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", retrieve("junitVersion"))
}
