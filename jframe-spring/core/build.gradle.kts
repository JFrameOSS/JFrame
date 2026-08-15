description = "JFrame Spring Core - Provides core utilities and application properties for the JFrame"
fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-core"))

    // ======= ANNOTATION PROCESSORS =======
    annotationProcessor("org.mapstruct:mapstruct-processor:${retrieve("mapStructVersion")}")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:${retrieve("mapStructVersion")}")

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-json")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.aspectj:aspectjweaver:${retrieve("aspectjVersion")}")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:${retrieve("springdocVersion")}")

    // ======= OTHER DEPENDENCIES =======
    api("org.mapstruct:mapstruct:${retrieve("mapStructVersion")}")

    // ======= PUBLISHED CONSTRAINTS =======
    // tools.jackson.databind 3.x requires jackson-annotations 2.22, but springdoc/swagger
    // transitively pulls jackson-bom 2.21.x which downgrades it, causing
    // NoClassDefFoundError: JsonApplyView at runtime for consumers.
    constraints {
        api("com.fasterxml.jackson.core:jackson-annotations:${retrieve("jacksonAnnotationsVersion")}") {
            because("tools.jackson.databind requires 2.22; springdoc transitively downgrades it via jackson-bom, causing NoClassDefFoundError: JsonApplyView")
        }
    }

    // ======= TEST =======
    testImplementation("tools.jackson.core:jackson-databind:${retrieve("jacksonVersion")}")
    testImplementation("org.springframework.security:spring-security-test")
}
