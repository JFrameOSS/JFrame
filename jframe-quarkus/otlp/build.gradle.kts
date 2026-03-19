description = "JFrame Quarkus OpenTelemetry - CDI interceptors for tracing and timing in Quarkus"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-quarkus-core"))

    // OpenTelemetry API — api (transitive to consumers)
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${retrieve("openTelemetryVersion")}"))
    api("io.opentelemetry", "opentelemetry-api")
    api("io.opentelemetry", "opentelemetry-context")

    // SmallRye Config — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))

    // Quarkus CDI — compileOnly
    compileOnly("jakarta.enterprise", "jakarta.enterprise.cdi-api", retrieve("jakartaCdiVersion"))
    compileOnly("jakarta.interceptor", "jakarta.interceptor-api", retrieve("jakartaInterceptorVersion"))
    compileOnly("jakarta.ws.rs", "jakarta.ws.rs-api", retrieve("jakartaWsrsVersion"))

    // Jackson — compileOnly (needed to resolve annotations from jframe-core)
    compileOnly("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))

    // Security
    compileOnly("io.quarkus", "quarkus-security", retrieve("quarkusVersion"))

    // Test dependencies
    testImplementation("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    testImplementation("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))
    testImplementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${retrieve("openTelemetryVersion")}"))
    testImplementation("io.opentelemetry", "opentelemetry-api")
    testImplementation("io.opentelemetry", "opentelemetry-context")
    testImplementation("io.opentelemetry", "opentelemetry-sdk-testing")
    testImplementation("jakarta.enterprise", "jakarta.enterprise.cdi-api", retrieve("jakartaCdiVersion"))
    testImplementation("jakarta.interceptor", "jakarta.interceptor-api", retrieve("jakartaInterceptorVersion"))
    testImplementation("jakarta.ws.rs", "jakarta.ws.rs-api", retrieve("jakartaWsrsVersion"))
    testImplementation("io.quarkus", "quarkus-security", retrieve("quarkusVersion"))
    testImplementation("org.junit.jupiter", "junit-jupiter", retrieve("junitVersion"))
    testImplementation("org.mockito", "mockito-core", retrieve("mockitoVersion"))
    testImplementation("org.mockito", "mockito-junit-jupiter", retrieve("mockitoVersion"))
    testImplementation("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))
    testImplementation("ch.qos.logback", "logback-classic", retrieve("logbackVersion"))
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", retrieve("junitVersion"))
}
