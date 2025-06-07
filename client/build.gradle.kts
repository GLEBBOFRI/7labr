plugins {
    id("java")
}

group = "org.example.client"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Зависимости для тестирования (только для клиента)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // GSON (если используется на клиенте)
    implementation("com.google.code.gson:gson:2.12.1")
}

tasks.test {
    useJUnitPlatform()
}

// Задача для сборки JAR-файла клиентского приложения
tasks.jar {
    archiveFileName.set("client-app.jar") // Имя выходного JAR-файла
    from(sourceSets.main.get().output) // ИСПРАВЛЕНО: используем get() для явного получения SourceSet

    // ИСПРАВЛЕНО: Добавляем .get() для явного разрешения runtimeClasspath
    // Это гарантирует, что зависимости будут разрешены до того, как мы попытаемся их использовать
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { project.zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    // Определяем основной класс для запуска клиента
    manifest {
        attributes(mapOf("Main-Class" to "org.example.ClientMain"))
    }
}
