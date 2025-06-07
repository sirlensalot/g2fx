plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "org.g2fx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("org.g2fx.g2gui.G2GuiApplication")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls")
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.1.2")

    implementation(project(":g2lib"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}

jlink {
    imageZip.set(file("${buildDir}/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}

//tasks.register("jlinkZip") {
//    group = "distribution"
//}

tasks.register<JavaExec>("runApp") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.g2fx.g2gui.HelloApplication"
}
