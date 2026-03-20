description = "JFrame Core - Framework-agnostic core models and utilities"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    // ======= API (transitive to consumers) =======
    api("org.apache.commons", "commons-lang3", retrieve("commonsLangVersion"))
    api("org.apache.commons", "commons-collections4", retrieve("commonsCollectionVersion"))
    api("commons-io", "commons-io", retrieve("commonsIoVersion"))

    // Hamcrest is used in public API signatures (FieldRejection, ValidationResult)
    api("org.hamcrest", "hamcrest", retrieve("hamcrestVersion"))

    // ======= COMPILE-ONLY (provided by consumer) =======
    compileOnly("jakarta.persistence", "jakarta.persistence-api", retrieve("jakartaPersistenceVersion"))
    compileOnly("jakarta.annotation", "jakarta.annotation-api", retrieve("jakartaAnnotationVersion"))
    compileOnly("io.swagger.core.v3", "swagger-annotations-jakarta", retrieve("swaggerVersion"))
    compileOnly("org.jspecify", "jspecify", retrieve("jspecifyVersion"))
    compileOnly("net.ttddyy", "datasource-proxy", retrieve("datasourceProxyVersion"))

    // ======= IMPLEMENTATION =======
    implementation("org.slf4j", "slf4j-api", retrieve("slf4jVersion"))
    implementation("tools.jackson.core", "jackson-databind", retrieve("jacksonVersion"))
    implementation("com.fasterxml.jackson.core", "jackson-annotations", retrieve("jacksonAnnotationsVersion"))

    // ======= TEST =======
    testImplementation("org.junit.jupiter", "junit-jupiter", retrieve("junitVersion"))
    testImplementation("org.mockito", "mockito-junit-jupiter", retrieve("mockitoVersion"))
    testImplementation("jakarta.persistence", "jakarta.persistence-api", retrieve("jakartaPersistenceVersion"))
    testImplementation("net.ttddyy", "datasource-proxy", retrieve("datasourceProxyVersion"))
    testCompileOnly("io.swagger.core.v3", "swagger-annotations-jakarta", retrieve("swaggerVersion"))
    testCompileOnly("jakarta.annotation", "jakarta.annotation-api", retrieve("jakartaAnnotationVersion"))
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", retrieve("junitVersion"))
    testRuntimeOnly("ch.qos.logback", "logback-classic", retrieve("logbackVersion"))
}
