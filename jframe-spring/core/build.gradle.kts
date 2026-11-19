description = "JFrame Spring Core - Provides core utilities and application properties for the JFrame"

dependencies {
    api(project(":jframe-core"))

    // ======= ANNOTATION PROCESSORS =======
    annotationProcessor(libs.mapstruct.processor)
    testAnnotationProcessor(libs.mapstruct.processor)

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-json")
    api("org.springframework.boot:spring-boot-starter-security")
    api(libs.aspectjweaver)
    api(libs.springdoc.webmvc.ui)

    // ======= OTHER DEPENDENCIES =======
    api(libs.mapstruct)

    // ======= PUBLISHED CONSTRAINTS =======
    // tools.jackson.databind 3.x requires jackson-annotations 2.22, but springdoc/swagger
    // transitively pulls jackson-bom 2.21.x which downgrades it, causing
    // NoClassDefFoundError: JsonApplyView at runtime for consumers.
    constraints {
        api(libs.jackson.annotations) {
            because("tools.jackson.databind requires 2.22; springdoc transitively downgrades it via jackson-bom, causing NoClassDefFoundError: JsonApplyView")
        }
    }

    // ======= TEST =======
    testImplementation(libs.jackson.databind)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-tomcat")
    testImplementation(testFixtures(project(":jframe-core")))
}
