import com.diffplug.gradle.spotless.SpotlessExtension
import org.cyclonedx.Version
import org.cyclonedx.gradle.CyclonedxAggregateTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.springframework.boot.gradle.tasks.bundling.BootJar
import ru.vyarus.gradle.plugin.quality.QualityExtension
import java.util.*
import java.util.Calendar.YEAR
import java.util.Objects.nonNull

// =============== PROJECT PROPERTIES =================
plugins {
    id("java") apply true
    id("java-library") apply true
    id("io.freefair.lombok") apply true

    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false

    id("com.github.vlsi.jandex") apply false

    id("com.diffplug.spotless") apply true
    id("project-report") apply true
    id("ru.vyarus.quality") apply true
    id("org.cyclonedx.bom") apply true
    id("com.github.spotbugs") apply true
    id("maven-publish") apply true
    id("signing") apply true
    id("io.github.ben-manes.versions") apply true
}

repositories {
    configureRepositories()
}

// =============== SUBPROJECTS CONFIGURATION =================
subprojects {
    project.group = retrieve("group")
    project.version = retrieve("version")

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "ru.vyarus.quality")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "io.github.ben-manes.versions")
    apply(plugin = "project-report")

    if (project.name.startsWith("jframe-spring-")) {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
    }

    if (project.name.startsWith("jframe-quarkus-")) {
        apply(plugin = "com.github.vlsi.jandex")
        afterEvaluate {
            tasks.matching { it.name == "checkstyleMain" || it.name == "pmdMain" || it.name == "spotbugsMain" }
                .configureEach { mustRunAfter(tasks.named("processJandexIndex")) }
        }
    }

    java {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
            implementation = JvmImplementation.VENDOR_SPECIFIC
            vendor = JvmVendorSpec.ADOPTIUM
        }
    }

    repositories {
        configureRepositories()
    }

    configurations.all {
        // jackson-annotations:2.22 is required by tools.jackson.databind:3.x but gets downgraded by
        // springdoc/swagger's jackson-bom:2.21.x constraint. Enforce the minimum required version.
        resolutionStrategy.force("com.fasterxml.jackson.core:jackson-annotations:${retrieve("jacksonAnnotationsVersion")}")
        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-annotations") {
                useVersion(retrieve("jacksonAnnotationsVersion"))
                because("tools.jackson.databind 3.x requires jackson-annotations 2.22; springdoc BOM downgrades to 2.21")
            }
        }
    }

    // Spring Boot's platform owns every `io.opentelemetry:*` coordinate. The instrumentation
    // artifacts JFrame pins are compiled against one specific API version - when Spring Boot pins an
    // older one they link against symbols that are not on the classpath and fail at runtime with
    // NoClassDefFoundError, invisible to compilation. Fail the build on any such downgrade.
    if (project.name.startsWith("jframe-spring-")) {
        val graph =
            configurations
                .named("runtimeClasspath")
                .flatMap { it.incoming.resolutionResult.rootComponent }

        val otelVersionGuard =
            tasks.register("otelVersionGuard") {
                group = "verification"
                description = "Fails if an io.opentelemetry module resolves below a requested version."
                doLast {
                    val downgrades = otelDowngrades(graph.get())
                    if (downgrades.isNotEmpty()) {
                        throw GradleException(
                            downgrades.joinToString(
                                separator = "\n  - ",
                                prefix =
                                    "OpenTelemetry version conflict. Align " +
                                        "openTelemetryInstrumentationVersion with the release train " +
                                        "targeting the API version Spring Boot pins:\n  - ",
                            ),
                        )
                    }
                }
            }

        tasks.named("check") { dependsOn(otelVersionGuard) }
    }

    dependencies {
        if (project.name.startsWith("jframe-spring-")) {
            annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
            compileOnly("org.springframework.boot:spring-boot-configuration-processor")
            compileOnly("org.springframework.boot:spring-boot-starter-web")

            // ======= TEST DEPENDENCIES =======
            testImplementation("jakarta.servlet:jakarta.servlet-api:${retrieve("jakartaServletVersion")}")
            testImplementation("org.springframework.boot:spring-boot-test")
            testImplementation("org.springframework.boot:spring-boot-starter-test") {
                exclude("com.vaadin.external.google", module = "android-json")
            }
        }
    }

    tasks.withType<JavaCompile> {
        if (!rootProject.hasProperty("disableAutoFormat")) {
            dependsOn("spotlessApply")
        }
        options.release = 21
        options.isDeprecation = true
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.addAll(
            arrayOf(
                "-Xlint:all",
                "-Xlint:-serial",
                "-Xlint:-processing",
                "-Xlint:-this-escape",
                "-Werror"
            )
        )
    }

    tasks.withType<Javadoc> {
        description = "Generates project-level Javadoc API documentation."
        options.memberLevel = JavadocMemberLevel.PROTECTED
        options.header = project.name

        val javadocOpts = options as CoreJavadocOptions
        javadocOpts.addBooleanOption("html5", true)
        javadocOpts.addStringOption("Xdoclint:none", "-quiet")
        javadocOpts.addStringOption("Xlint:none")

        logging.captureStandardError(LogLevel.INFO)
        logging.captureStandardOutput(LogLevel.INFO)
    }

    tasks.findByName("bootJar")?.let {
        // Disable bootJar (they should be libraries, not applications)
        (it as BootJar).enabled = false
    }

    tasks.getByName<Jar>("jar") {
        manifest.attributes["Implementation-Title"] = project.name
        manifest.attributes["Implementation-Version"] = project.version
        archiveBaseName.set(project.name)
        archiveFileName.set(project.name + ".jar")
        archiveClassifier.set("")

        from("${rootProject.projectDir}/src/dist") {
            include("license.txt")
            include("notice.txt")
            include("CHANGELOG.md")
            into("META-INF")
            val replace = mapOf("copyright" to Calendar.getInstance().get(YEAR), "version" to rootProject.version)
            expand(replace)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showCauses = true
            showExceptions = true
            events = setOf(
                TestLogEvent.FAILED,
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED
            )
        }
    }

    afterEvaluate {
        val disabledTasks = project.findProperty("disableTasks")?.toString()?.split(",") ?: emptyList()
        disabledTasks.forEach { taskPattern ->
            tasks.matching { it.name.contains(taskPattern.trim()) }.configureEach {
                enabled = false
                logger.lifecycle("Disabled task: $name in project: ${project.name}")
            }
        }
    }

    configure<SpotlessExtension> {
        spotless {
            java {
                cleanthat()
                toggleOffOn()
                target("src/main/java/**/*.java", "src/test/java/**/*.java")
                eclipse().configFile("${rootDir}/src/quality/config/spotless/styling.xml")
                endWithNewline()
                removeUnusedImports()
                trimTrailingWhitespace()
                importOrder("", "java|jakarta|javax", "groovy", "org", "com", "\\#")
            }
        }
    }

    configure<QualityExtension> {
        autoRegistration = true
        configDir = "${rootDir}/src/quality/config/"

        spotbugsVersion = retrieve("spotbugsVersion")
        spotbugs = true

        pmdVersion = retrieve("pmdVersion")
        pmd = true

        checkstyleVersion = retrieve("checkstyleVersion")
        checkstyle = true

        codenarcVersion = retrieve("codenarcVersion")
        codenarc = true
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("java") {
                artifactId = project.name
                groupId = project.group.toString()
                version = project.version.toString()

                from(components["java"])
                pom {
                    packaging = "jar"
                    name.set("JFrame - " + project.name)
                    description.set("JFrame - " + project.name)
                    url.set(retrieve("url"))

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            name.set("Jordi Jaspers")
                            email.set("jordijaspers@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/JFrameOSS/JFrame.git")
                        developerConnection.set("scm:git:ssh://github.com:JFrameOSS/JFrame.git")
                        url.set(retrieve("url"))
                    }
                }
            }
        }
        repositories {
            maven {
                name = "MavenLocal"
                url = uri(mavenLocal().url)
            }
        }
    }

    // Standard Gradle signing
    configure<SigningExtension> {
        val signingKey = System.getenv("SIGNING_KEY")
        val signingPassword = System.getenv("SIGNING_PASSWORD")
        if (nonNull(signingKey) && nonNull(signingPassword)) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(extensions.getByType<PublishingExtension>().publications["java"])
        }
    }
}

// =============== CYCLONEDX SBOM CONFIGURATION =================
// The root `cyclonedxBom` task aggregates the per-project `cyclonedxDirectBom` outputs, so those
// must stay enabled in the subprojects. Only the root project's own direct BOM is redundant.
tasks.configureEach {
    if (name == "cyclonedxDirectBom") {
        enabled = false
    }
}

// Aggregated SBOM — single BOM for the entire project hierarchy
tasks.named<CyclonedxAggregateTask>("cyclonedxBom") {
    projectType = org.cyclonedx.model.Component.Type.LIBRARY
    schemaVersion = Version.VERSION_16
    componentName = rootProject.name
    componentVersion = rootProject.version.toString()
    includeBomSerialNumber = true
    includeLicenseText = true
}

// =============== OPTIONAL FUNCTIONS / TASKS =================
fun Project.artifactCoordinates() = "${group}:${name}:${version}"
tasks.register<Delete>("cleanLocalMavenArtifacts") {
    group = "publishing"
    description = "Cleans local Maven artifacts from ~/.m2/repository"

    val mavenLocal = File(System.getProperty("user.home"), ".m2/repository")
    val artifactGroup = project.group.toString()
        .replace('.', '/')
        .removeSurrounding("\"", "\"")

    val artifactPath = File(mavenLocal, artifactGroup)
    doFirst {
        if (artifactPath.exists()) {
            logger.lifecycle("🧹 Cleaning existing artifacts from: ${artifactPath.absolutePath}")
        } else {
            logger.lifecycle("ℹ️  No existing artifacts found at: ${artifactPath.absolutePath}")
        }
    }

    delete(artifactPath)
    doLast {
        if (artifactPath.exists()) {
            logger.lifecycle("✅ Local Maven artifacts cleaned")
        }
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Cleans and publishes all modules to the local Maven repository"
    dependsOn("clean", "cleanLocalMavenArtifacts")
    subprojects.forEach { subproject ->
        dependsOn(":${subproject.name}:build")
        dependsOn(":${subproject.name}:publishToMavenLocal")
    }

    doLast {
        logger.lifecycle("📦 Published to local repository (~/.m2/repository) :")
        subprojects.forEach { logger.lifecycle("   ✓ ${it.artifactCoordinates()}") }
        logger.lifecycle("🎉 Successfully published all modules locally!")
    }
}

tasks.register("publishMaven") {
    group = "publishing"
    description = "Publishes all build modules to the configured Maven repositories"
    doFirst {
        logger.lifecycle(
            "|ℹ️ Publishing artifacts: ${project.group}:${project.version}".trimMargin().replace("\"", "")
        )
    }

    subprojects.forEach { subproject ->
        dependsOn(":${subproject.name}:publish")
    }

    doLast {
        logger.lifecycle("📦 Published artifacts:")
        subprojects.forEach { logger.lifecycle("   ✓ ${it.artifactCoordinates()}") }
        logger.lifecycle("🎉 Successfully published all modules!")
    }
}

fun RepositoryHandler.configureRepositories() {
    mavenCentral()
    mavenLocal()
}

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

/**
 * Reports every `io.opentelemetry:*` module that some dependency requested at a higher version than
 * the one actually selected. Only downgrades matter - the requesting artifact is then compiled
 * against an API that is not on the classpath. Upgrades are safe, the API is backward compatible.
 */
fun otelDowngrades(root: ResolvedComponentResult): List<String> {
    // `1.62.0-alpha` is the same release line as `1.62.0`; the suffix marks an incubating artifact.
    fun parse(version: String) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    val found = sortedSetOf<String>()
    val seen = mutableSetOf<ComponentIdentifier>()

    fun visit(component: ResolvedComponentResult) {
        if (!seen.add(component.id)) return
        component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { edge ->
            val selected = edge.selected.moduleVersion
            val requested = edge.requested
            if (selected != null && selected.group == "io.opentelemetry" && requested is ModuleComponentSelector) {
                val firstDifference =
                    parse(requested.version).zip(parse(selected.version)).firstOrNull { it.first != it.second }
                if (firstDifference != null && firstDifference.first > firstDifference.second) {
                    found.add(
                        "${selected.name} requested at ${requested.version} but resolved to " +
                            "${selected.version} (requested by ${component.id})",
                    )
                }
            }
            visit(edge.selected)
        }
    }

    visit(root)
    return found.toList()
}
