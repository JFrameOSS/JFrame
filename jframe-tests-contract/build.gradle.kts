description = "JFrame Tests Contract - Shared test fixtures and verification utilities for cross-framework contract testing"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-core"))
    implementation("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    implementation("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))
}
