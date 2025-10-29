description = "JFrame OpenTelemetry Starter - Provides OpenTelemetry integration with custom tracing, logging, and monitoring capabilities"
fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":starter-core"))

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot", "spring-boot-starter-web")
    api("org.springframework.boot", "spring-boot-starter-webflux")
    api("org.springframework.boot", "spring-boot-starter-security")

    // ======= OTEL DEPENDENCIES (https://opentelemetry.io/docs/getting-started/) =======
    api("org.apache.httpcomponents.client5", "httpclient5", retrieve("httpClientVersion"))
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:" + retrieve("openTelemetryVersion")))
    api("io.opentelemetry.instrumentation", "opentelemetry-spring-boot-starter")
    api("io.opentelemetry", "opentelemetry-extension-trace-propagators")
    api("io.opentelemetry.semconv", "opentelemetry-semconv")

    // ======= OTHER DEPENDENCIES =======
    compileOnly("org.apache.commons", "commons-lang3", retrieve("commonsLangVersion"))
    compileOnly("jakarta.servlet", "jakarta.servlet-api", retrieve("jakartaServletVersion"))
    compileOnly("org.aspectj", "aspectjweaver", retrieve("aspectjVersion"))
    compileOnly("org.aspectj", "aspectjweaver", retrieve("aspectjVersion"))
}
