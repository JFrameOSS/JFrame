description = "JFrame Core - Framework-agnostic core models and utilities"

plugins {
    `java-test-fixtures`
}

// Test fixtures are for jFrame's own tests only; never publish them.
listOf("testFixturesApiElements", "testFixturesRuntimeElements").forEach {
    (components["java"] as AdhocComponentWithVariants).withVariantsFromConfiguration(configurations[it]) { skip() }
}

dependencies {
    // ======= API (transitive to consumers) =======
    api(libs.commons.lang3)
    api(libs.commons.collections4)
    // Hamcrest is used in public API signatures (FieldRejection, ValidationResult)
    api(libs.hamcrest)

    // Exposed in public API: HttpException.getHttpStatus() returns Response.Status
    api(libs.jakarta.wsrs.api)

    // ======= COMPILE-ONLY (provided by consumer) =======
    compileOnly(libs.jakarta.persistence.api)
    compileOnly(libs.jakarta.annotation.api)
    // OpenAPI schema-name annotations: visible to consumers' compilers (avoids missing-annotation warnings), never at runtime
    compileOnlyApi(libs.swagger.annotations)
    compileOnlyApi(libs.microprofile.openapi.api)
    compileOnly(libs.jspecify)
    compileOnly(libs.datasource.proxy)

    // ======= IMPLEMENTATION =======
    implementation(libs.slf4j.api)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)

    // ======= TEST FIXTURES (shared RFC 9457 schema + expected bodies) =======
    testFixturesImplementation(libs.jackson.databind)
    testFixturesImplementation(libs.hamcrest)
    testFixturesImplementation(libs.json.schema.validator)

    // ======= TEST =======
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.jakarta.persistence.api)
    testImplementation(libs.datasource.proxy)
    testCompileOnly(libs.swagger.annotations)
    testCompileOnly(libs.jakarta.annotation.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)
}
