description = "JFrame Quarkus OTLP - OpenTelemetry tracing interceptors and configuration for Quarkus"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

tasks.named<ProcessResources>("processResources") {
    filesMatching("META-INF/quarkus-extension.properties") {
        expand("version" to project.version)
    }
}

dependencies {
    api(project(":jframe-quarkus-core"))

    // Jackson annotations — compileOnly (transitive from jframe-core, not visible via quarkus-core chain)
    compileOnly("com.fasterxml.jackson.core", "jackson-annotations", retrieve("jacksonAnnotationsVersion"))

    // Quarkus APIs — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly("jakarta.enterprise", "jakarta.enterprise.cdi-api", retrieve("jakartaCdiVersion"))
    compileOnly("jakarta.interceptor", "jakarta.interceptor-api", retrieve("jakartaInterceptorVersion"))
    compileOnly("io.quarkus", "quarkus-arc", retrieve("quarkusVersion"))
    compileOnly("io.quarkus", "quarkus-core", retrieve("quarkusVersion"))
    compileOnly("io.quarkus", "quarkus-opentelemetry", retrieve("quarkusVersion"))
    compileOnly("io.quarkus", "quarkus-security", retrieve("quarkusVersion"))

    // Test dependencies
    testImplementation("com.fasterxml.jackson.core", "jackson-annotations", retrieve("jacksonAnnotationsVersion"))
    testImplementation("org.junit.jupiter", "junit-jupiter", retrieve("junitVersion"))
    testImplementation("org.mockito", "mockito-core", retrieve("mockitoVersion"))
    testImplementation("org.mockito", "mockito-junit-jupiter", retrieve("mockitoVersion"))
    testImplementation("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))
    testImplementation("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))
    testImplementation("io.smallrye.config", "smallrye-config", retrieve("smallryeConfigVersion"))
    testImplementation("io.quarkus", "quarkus-opentelemetry", retrieve("quarkusVersion"))
    testImplementation("io.quarkus", "quarkus-security", retrieve("quarkusVersion"))
    testImplementation("jakarta.interceptor", "jakarta.interceptor-api", retrieve("jakartaInterceptorVersion"))
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", retrieve("junitVersion"))
}
