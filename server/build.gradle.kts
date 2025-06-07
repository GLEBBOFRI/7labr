// build.gradle.kts (Kotlin DSL)
plugins {
    id("java")
}

group = "org.example.server"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.postgresql:postgresql:42.7.3")

    implementation("com.google.code.gson:gson:2.12.1")
}

tasks.test {
    useJUnitPlatform()
}

// Задача для сборки JAR-файла серверного приложения
tasks.jar {
    archiveFileName.set("server-app.jar")
    from(sourceSets.main.get().output) // Используем get() для явного получения SourceSet

    // ИСПРАВЛЕНО: Добавляем .get() для явного разрешения runtimeClasspath
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { project.zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes(mapOf("Main-Class" to "org.example.server.ServerMain"))
    }
}
