description = "JFrame Quarkus OTLP - OpenTelemetry tracing interceptors and configuration for Quarkus"

tasks.named<ProcessResources>("processResources") {
    val projectVersion = project.version.toString()
    filesMatching("META-INF/quarkus-extension.properties") {
        expand("version" to projectVersion)
    }
}

dependencies {
    api(project(":jframe-quarkus-core"))

    // Jackson annotations — compileOnly (transitive from jframe-core, not visible via quarkus-core chain)
    compileOnly(libs.jackson.annotations)

    // Quarkus APIs — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly(libs.quarkus.arc)
    compileOnly(libs.quarkus.core)
    compileOnly(libs.quarkus.opentelemetry)
    compileOnly(libs.quarkus.security)

    // Test dependencies
    testImplementation(libs.jackson.annotations)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.smallrye.config)
    testImplementation(libs.quarkus.opentelemetry)
    testImplementation(libs.quarkus.security)
    testRuntimeOnly(libs.junit.platform.launcher)
}
