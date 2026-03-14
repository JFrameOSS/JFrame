description = "JFrame Tests Spring - Spring Boot integration tests verifying exception handling and validation contract behavior"

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:${retrieve("springBootPluginVersion")}"))
    testImplementation(project(":jframe-tests-contract"))
    testImplementation(project(":jframe-spring-core"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter:${retrieve("junitVersion")}")
    testImplementation("org.hamcrest:hamcrest:${retrieve("hamcrestVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
