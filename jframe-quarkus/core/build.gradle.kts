description = "JFrame Quarkus Core - JAX-RS filters, exception mappers, and CDI utilities for Quarkus"

dependencies {
    api(project(":jframe-core"))

    // Quarkus APIs — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly(libs.jakarta.validation.api)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.smallrye.config.core)
    compileOnly(libs.smallrye.openapi.core)
    compileOnly(libs.quarkus.smallrye.openapi)
    compileOnly(libs.quarkus.arc)
    compileOnly(libs.quarkus.core)

    // Test dependencies
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jakarta.validation.api)
    testImplementation(libs.quarkus.arc)
    testImplementation(libs.quarkus.smallrye.openapi)
    testImplementation(libs.quarkus.core)
    testImplementation(libs.smallrye.openapi.jaxrs)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly(libs.logback.classic)
    testImplementation(libs.resteasy.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(testFixtures(project(":jframe-core")))
}
