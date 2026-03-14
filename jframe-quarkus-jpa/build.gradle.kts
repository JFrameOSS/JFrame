description = "JFrame Quarkus JPA - Panache search specification adapter and page utilities for Quarkus"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-quarkus-core"))

    // Quarkus Panache API — compileOnly (provided by consumer)
    compileOnly("io.quarkus:quarkus-hibernate-orm-panache:${retrieve("quarkusVersion")}")
    // Swagger annotations on jframe-core API — compileOnly (annotation types needed to compile against core classes)
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:${retrieve("swaggerVersion")}")

    // Test dependencies
    testImplementation("io.quarkus:quarkus-hibernate-orm-panache:${retrieve("quarkusVersion")}")
    testImplementation("io.swagger.core.v3:swagger-annotations-jakarta:${retrieve("swaggerVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${retrieve("junitVersion")}")
    testImplementation("org.mockito:mockito-core:${retrieve("mockitoVersion")}")
    testImplementation("org.mockito:mockito-junit-jupiter:${retrieve("mockitoVersion")}")
    testImplementation("org.hamcrest:hamcrest:${retrieve("hamcrestVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${retrieve("junitVersion")}")
}
