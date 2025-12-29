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
    api("org.springframework.boot", "spring-boot-starter-json")
    api("org.springframework.boot", "spring-boot-starter-security")
    api("org.springframework.boot", "spring-boot-starter-aop", retrieve("springAopVersion"))
    api("org.springdoc", "springdoc-openapi-starter-webmvc-ui", retrieve("springdocVersion"))

    api("net.logstash.logback", "logstash-logback-encoder", retrieve("logstashEncoderVersion"))
    api("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))

    api("commons-io", "commons-io", retrieve("commonsIoVersion"))
    api("org.apache.commons", "commons-collections4", retrieve("commonsCollectionVersion"))
    api("org.apache.commons", "commons-lang3", retrieve("commonsLangVersion"))

    // ======= OTHER DEPENDENCIES =======
    implementation("org.mapstruct","mapstruct", retrieve("mapStructVersion"))
}
