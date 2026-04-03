description = "JFrame Spring JPA - Provides utilities for pagination and searching via JPA."
fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":jframe-spring-core"))

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot", "spring-boot-starter-jdbc")
    api("org.springframework.boot", "spring-boot-starter-data-jpa")

    // ======= OTHER DEPENDENCIES =======
    implementation("net.ttddyy", "datasource-proxy", retrieve("datasourceProxyVersion"))
}
