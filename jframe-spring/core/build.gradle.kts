description = "JFrame Spring Core - Provides core utilities and application properties for the JFrame"
fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-core"))

    // ======= ANNOTATION PROCESSORS =======
    annotationProcessor("org.mapstruct", "mapstruct-processor", retrieve("mapStructVersion"))
    annotationProcessor("org.springframework.boot", "spring-boot-configuration-processor")

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot", "spring-boot-starter")
    api("org.springframework.boot", "spring-boot-starter-json")
    api("org.springframework.boot", "spring-boot-starter-security")
    api("org.springframework.boot", "spring-boot-starter-aop", retrieve("springAopVersion"))
    api("org.springdoc", "springdoc-openapi-starter-webmvc-ui", retrieve("springdocVersion"))

    api("net.logstash.logback", "logstash-logback-encoder", retrieve("logstashEncoderVersion"))

    // ======= OTHER DEPENDENCIES =======
    implementation("org.mapstruct","mapstruct", retrieve("mapStructVersion"))

    // ======= TEST =======
    testImplementation("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    testImplementation("org.springframework.security", "spring-security-test")
}
