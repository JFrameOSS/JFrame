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

    // Runtime extensions this extension builds on — api so consumers get them transitively
    // (deployment counterparts are declared in otlp-deployment).
    api(libs.quarkus.arc)
    api(libs.quarkus.core)
    api(libs.quarkus.opentelemetry)
    // Optional: SecurityIdentity is resolved via Instance<>; consumers opt into security themselves.
    compileOnly(libs.quarkus.security)

    // CVE floor: guava/protobuf are transitive via quarkus-opentelemetry gRPC; quarkus-bom pins older versions.
    constraints {
        api(libs.guava)
        api(libs.protobuf.java)
    }

    // Test dependencies
    testImplementation(libs.jackson.annotations)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.smallrye.config)
    testImplementation(libs.quarkus.security)
    testRuntimeOnly(libs.junit.platform.launcher)
}
