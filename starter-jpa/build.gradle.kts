description = "JFrame Starter JPA - Provides utilities for pagination and searching via JPA."
fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

dependencies {
    api(project(":starter-core"))

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot", "spring-boot-starter-web")
    api("org.springframework.boot", "spring-boot-starter-jdbc")
    api("org.springframework.boot", "spring-boot-starter-data-jpa")

    // ======= OTHER DEPENDENCIES =======
    implementation("net.ttddyy", "datasource-proxy", retrieve("datasourceProxyVersion"))
    implementation("org.aspectj", "aspectjweaver", retrieve("aspectjVersion"))

    implementation("org.glassfish.jaxb", "jaxb-runtime", retrieve("jaxbVersion"))
    implementation("jakarta.xml.bind", "jakarta.xml.bind-api", retrieve("jakartaXmlBindVersion"))

    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-json-org", retrieve("jacksonVersion"))
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", retrieve("jacksonVersion"))

    implementation("org.apache.commons", "commons-text", retrieve("commonsTextVersion"))
}
