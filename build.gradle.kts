plugins {
    id("org.gradle.presentation.asciidoctor")
    `java-library`
}

repositories {
    maven {
        url = uri("https://repo.gradle.org/gradle/libs")
    }
}

dependencies {
    testImplementation("junit:junit:4.12")
    testImplementation("org.gradle:sample-check:0.7.0")
    testImplementation(gradleTestKit())
}

presentation {
    githubUserName.set("jlstrater")
    githubRepoName.set("test-driven-docs-microxchg")
}
