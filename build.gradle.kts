import com.diffplug.gradle.spotless.SpotlessExtension
import org.cyclonedx.Version
import org.cyclonedx.gradle.CyclonedxDirectTask
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jreleaser.gradle.plugin.JReleaserExtension
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

    id("com.diffplug.spotless") apply true
    id("project-report") apply true
    id("ru.vyarus.quality") apply true
    id("org.cyclonedx.bom") apply true
    id("com.github.spotbugs") apply true
    id("maven-publish") apply true
    id("signing") apply true
    id("com.github.ben-manes.versions") apply true
    id("org.jreleaser") apply true
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
    apply(plugin = "org.cyclonedx.bom")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "project-report")

    if (project.name.startsWith("starter-")) {
        apply(plugin = "org.springframework.boot")
        apply(plugin = "io.spring.dependency-management")
    }

    java {
        withJavadocJar()
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
            implementation = JvmImplementation.VENDOR_SPECIFIC
            vendor = JvmVendorSpec.ADOPTIUM
        }
    }

    repositories {
        configureRepositories()
    }

    dependencies {
        if (project.name.startsWith("starter-")) {
            annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
            compileOnly("org.springframework.boot:spring-boot-configuration-processor")
            compileOnly("org.springframework.boot:spring-boot-starter-web")

            // ======= TEST DEPENDENCIES =======
            testImplementation("jakarta.servlet", "jakarta.servlet-api", retrieve("jakartaServletVersion"))
            testImplementation("org.springframework.boot", "spring-boot-test")
            testImplementation("org.springframework.boot", "spring-boot-starter-test") {
                exclude("com.vaadin.external.google", module = "android-json")
            }
        }
    }

    tasks.withType<JavaCompile> {
        dependsOn("spotlessApply")
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

    tasks.getByName<BootJar>("bootJar") {
        // Disable bootJar (they should be libraries, not applications)
        enabled = false
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

    tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
        projectType = org.cyclonedx.model.Component.Type.LIBRARY
        schemaVersion = Version.VERSION_16
        componentName = project.name
        componentVersion = project.version.toString()
        skipConfigs = listOf(".*test.*", ".*Test.*")
        jsonOutput = project.file("build/reports/sbom/${project.name}-sbom.json")
        xmlOutput = project.file("build/reports/sbom/${project.name}-sbom.xml")

        includeBomSerialNumber = true
        includeLicenseText = true
        includeMetadataResolution = true
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
                name = "StagingDeploy"
                url = uri(layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }

    configure<SigningExtension> {
        val signingKey = System.getenv("SIGNING_KEY")
        val signingPassword = System.getenv("SIGNING_PASSWORD")
        if (nonNull(signingKey) && nonNull(signingPassword)) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(extensions.getByType<PublishingExtension>().publications["java"])
        }
    }
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
    description = "Publishes all build modules to Maven Central via JReleaser"
    doFirst {
        logger.lifecycle(
            "|ℹ️ Publishing artifacts: ${project.group}:${project.version}".trimMargin().replace("\"", "")
        )
    }

    subprojects.forEach { subproject ->
        dependsOn(":${subproject.name}:publish")
    }

    dependsOn("jreleaserDeploy")
    doLast {
        logger.lifecycle("📦 Published artifacts:")
        subprojects.forEach { logger.lifecycle("   ✓ ${it.artifactCoordinates()}") }
        logger.lifecycle("🎉 Successfully published all modules to Maven Central!")
    }
}

fun RepositoryHandler.configureRepositories() {
    mavenCentral()
    mavenLocal()
}

fun retrieve(property: String): String =
    project.findProperty(property)?.toString()?.replace("\"", "")
        ?: throw IllegalStateException("Property $property not found")

// =============== JRELEASER CONFIGURATION =================
configure<JReleaserExtension> {
    project {
        name = rootProject.name
        description = "JFrame - A multi-module Java library framework built with Gradle and Spring Boot"
        authors = listOf("Jordi Jaspers")
        license = "Apache-2.0"
        copyright = "2024-${Calendar.getInstance().get(YEAR)} Jordi Jaspers"
    }

    // Signing configuration
    signing {
        armored = true
        active = org.jreleaser.model.Active.ALWAYS
        mode = org.jreleaser.model.Signing.Mode.MEMORY
    }

    // Maven Central deployment
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = org.jreleaser.model.Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"

                    // Staging directory where artifacts are collected
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())

                    // Retry configuration
                    retryDelay = 60
                    maxRetries = 3
                }
            }
        }
    }
}
