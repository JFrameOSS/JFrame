description = "JFrame Quarkus OTLP Deployment - Build-time processing for OpenTelemetry extension"

dependencies {
    implementation(project(":jframe-quarkus-otlp"))

    // Quarkus deployment dependencies — must be 'implementation' so they appear in the
    // published POM; the Quarkus extension resolver needs them on the deployment classpath.
    implementation(libs.quarkus.core.deployment)
    implementation(libs.quarkus.arc.deployment)
    implementation(libs.quarkus.opentelemetry.deployment)
}
