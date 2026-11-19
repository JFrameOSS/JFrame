description = "JFrame Spring OpenTelemetry - Provides OpenTelemetry integration with custom tracing, logging, and monitoring capabilities"

dependencies {
    api(project(":jframe-spring-core"))

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-webflux")

    // ======= OTEL DEPENDENCIES (https://opentelemetry.io/docs/getting-started/) =======
    api(libs.httpclient5)
    api(libs.opentelemetry.spring.boot.starter)
    api(libs.opentelemetry.semconv)

    // ======= OTHER DEPENDENCIES =======
    compileOnly(libs.jakarta.servlet.api)
    compileOnly(libs.aspectjweaver)
    testImplementation(testFixtures(project(":jframe-core")))
}
