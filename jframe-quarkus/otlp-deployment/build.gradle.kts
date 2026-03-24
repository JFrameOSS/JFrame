description = "JFrame Quarkus OTLP Deployment - Build-time processing for OpenTelemetry extension"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    implementation(project(":jframe-quarkus-otlp"))

    // Quarkus deployment dependencies
    compileOnly("io.quarkus", "quarkus-core-deployment", retrieve("quarkusVersion"))
    compileOnly("io.quarkus", "quarkus-arc-deployment", retrieve("quarkusVersion"))
}
