description = "JFrame Spring JPA - Provides utilities for pagination and searching via JPA."

dependencies {
    api(project(":jframe-spring-core"))

    // ======= NECESSARY SPRING DEPENDENCIES =======
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // ======= OTHER DEPENDENCIES =======
    implementation(libs.datasource.proxy)
}
