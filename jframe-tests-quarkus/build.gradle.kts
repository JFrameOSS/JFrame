description = "JFrame Tests Quarkus - Quarkus integration tests verifying exception handling and validation contract behavior"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    testImplementation(project(":jframe-tests-contract"))
    testImplementation(project(":jframe-quarkus-core"))

    // JAX-RS API (needed to invoke mappers directly)
    testImplementation("jakarta.ws.rs:jakarta.ws.rs-api:${retrieve("jakartaWsrsVersion")}")

    // RESTEasy core provides JAX-RS Response implementation at test runtime
    testRuntimeOnly("org.jboss.resteasy:resteasy-core:${retrieve("resteasyCoreVersion")}")

    // Standard test deps
    testImplementation("org.junit.jupiter:junit-jupiter:${retrieve("junitVersion")}")
    testImplementation("org.hamcrest:hamcrest:${retrieve("hamcrestVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${retrieve("junitVersion")}")
}
