// Settings plugins resolve before the version catalog exists, so nmcp's version lives here.
plugins {
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

nmcpSettings {
    centralPortal {
        username = System.getenv("MAVEN_USERNAME") ?: ""
        password = System.getenv("MAVEN_PASSWORD") ?: ""
        publishingType = "AUTOMATIC"
    }
}

rootProject.name = "jframe"

include("jframe-core")

// Spring modules — grouped under jframe-spring/
include("jframe-spring-core")
project(":jframe-spring-core").projectDir = file("jframe-spring/core")

include("jframe-spring-jpa")
project(":jframe-spring-jpa").projectDir = file("jframe-spring/jpa")

include("jframe-spring-otlp")
project(":jframe-spring-otlp").projectDir = file("jframe-spring/otlp")

// Quarkus modules — grouped under jframe-quarkus/
include("jframe-quarkus-core")
project(":jframe-quarkus-core").projectDir = file("jframe-quarkus/core")

include("jframe-quarkus-jpa")
project(":jframe-quarkus-jpa").projectDir = file("jframe-quarkus/jpa")

include("jframe-quarkus-otlp")
project(":jframe-quarkus-otlp").projectDir = file("jframe-quarkus/otlp")

include("jframe-quarkus-otlp-deployment")
project(":jframe-quarkus-otlp-deployment").projectDir = file("jframe-quarkus/otlp-deployment")
