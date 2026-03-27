description = "JFrame Quarkus OTLP Deployment - Build-time processing for OpenTelemetry extension"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    implementation(project(":jframe-quarkus-otlp"))

    // Quarkus deployment dependencies — must be 'implementation' so they appear in the
    // published POM; the Quarkus extension resolver needs them on the deployment classpath.
    implementation("io.quarkus", "quarkus-core-deployment", retrieve("quarkusVersion"))
    implementation("io.quarkus", "quarkus-arc-deployment", retrieve("quarkusVersion"))
}
