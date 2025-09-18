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

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}


javafx {
    version = "21"
    modules = listOf("javafx.controls")
}


application {
    mainClass.set("org.g2fx.g2gui.G2GuiApplication")
}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(files("libs/libusb4java-1.3.0-darwin-aarch64.jar"))
    implementation("org.usb4java:usb4java:1.3.0")
    //implementation("org.yaml:snakeyaml:2.3")
    //implementation("info.picocli:picocli:4.7.6")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.1")
    implementation("org.jline:jline:3.26.2")

    implementation("com.google.guava:guava:33.4.8-jre")


    implementation("org.controlsfx:controlsfx:11.1.2")

    implementation("org.fxmisc.richtext:richtextfx:0.11.6")

}

tasks.test {
    useJUnitPlatform()
}


tasks.register<JavaExec>("runApp") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "org.g2fx.g2gui.G2GuiApplication"
    jvmArgs = listOf(
        "--module-path", configurations.runtimeClasspath.get().asPath,
        "--add-modules", "javafx.controls"
    )
}
