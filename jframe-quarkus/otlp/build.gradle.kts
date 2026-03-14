description = "JFrame Quarkus OpenTelemetry - CDI interceptors for tracing and timing in Quarkus"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-quarkus-core"))

    // OpenTelemetry API — compileOnly (provided by consumer's quarkus-opentelemetry)
    compileOnly(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${retrieve("openTelemetryVersion")}"))
    compileOnly("io.opentelemetry", "opentelemetry-api")
    compileOnly("io.opentelemetry", "opentelemetry-context")

    // SmallRye Config — compileOnly (provided by consumer's Quarkus runtime)
    compileOnly("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))
    compileOnly("io.smallrye.config", "smallrye-config-common", retrieve("smallryeConfigVersion"))

    // Quarkus CDI — compileOnly
    compileOnly("jakarta.enterprise", "jakarta.enterprise.cdi-api", retrieve("jakartaCdiVersion"))
    compileOnly("jakarta.interceptor", "jakarta.interceptor-api", retrieve("jakartaInterceptorVersion"))
    compileOnly("jakarta.ws.rs", "jakarta.ws.rs-api", retrieve("jakartaWsrsVersion"))

    // Security
    compileOnly("io.quarkus", "quarkus-security", retrieve("quarkusVersion"))

    // Test dependencies
    testImplementation("io.smallrye.config", "smallrye-config-core", retrieve("smallryeConfigVersion"))
    testImplementation("io.smallrye.config", "smallrye-config-common", retrieve("smallryeConfigVersion"))
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
    testImplementation("org.assertj", "assertj-core", retrieve("assertjVersion"))
    testImplementation("org.slf4j", "slf4j-api", retrieve("slf4jVersion"))
    testImplementation("ch.qos.logback", "logback-classic", retrieve("logbackVersion"))
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", retrieve("junitVersion"))
}
