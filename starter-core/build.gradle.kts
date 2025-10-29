description = "JFrame Core - Provides core utilities and application properties for the JFrame"
fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    // ======= ANNOTATION PROCESSORS =======
    annotationProcessor("org.mapstruct", "mapstruct-processor", retrieve("mapStructVersion"))
    annotationProcessor("org.springframework.boot", "spring-boot-configuration-processor")

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot", "spring-boot-starter")
    api("org.springframework.boot", "spring-boot-starter-aop")
    api("org.springframework.boot", "spring-boot-starter-security")
    api("org.springdoc", "springdoc-openapi-starter-webmvc-ui", retrieve("springdocVersion"))

    api("commons-io", "commons-io", retrieve("commonsIoVersion"))
    api("net.logstash.logback", "logstash-logback-encoder", retrieve("logstashEncoderVersion"))
    api("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))

    // ======= OTHER DEPENDENCIES =======
    implementation("org.mapstruct","mapstruct", retrieve("mapStructVersion"))
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", retrieve("jacksonVersion"))
}
