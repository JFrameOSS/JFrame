description = "JFrame Quarkus JPA - Panache search specification adapter and page utilities for Quarkus"

dependencies {
    api(project(":jframe-quarkus-core"))

    // Quarkus Panache API — compileOnly (provided by consumer)
    compileOnly(libs.quarkus.hibernate.orm.panache)
    // Jackson annotations on jframe-core API — compileOnly (annotation types on PageResource/AppliedSort)
    compileOnly(libs.jackson.annotations)
    // Swagger annotations on jframe-core API — compileOnly (annotation types needed to compile against core classes)
    compileOnly(libs.swagger.annotations)

    // Agroal DataSource API — compileOnly (provided by consumer's quarkus-agroal)
    compileOnly("io.agroal:agroal-api")

    // DataSource proxy — CDI producer for SQL query logging
    implementation(libs.datasource.proxy)

    // Test dependencies
    testImplementation("io.agroal:agroal-api")
    testImplementation(libs.quarkus.hibernate.orm.panache)
    testImplementation(libs.swagger.annotations)
    testImplementation(libs.jackson.annotations)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
