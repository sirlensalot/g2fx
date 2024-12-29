plugins {
    java
}

group = "g2lib"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher");
    implementation(files("libs/libusb4java-1.3.0-darwin-aarch64.jar"))
    implementation("org.usb4java:usb4java:1.3.0")
    implementation("org.yaml:snakeyaml:2.3")
    // implementation("info.picocli:picocli:4.7.6")
    // implementation("org.jline:jline:3.26.2")
}

tasks.test {
    useJUnitPlatform()
}


tasks.register<JavaExec>("runApp") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "g2lib.Main"
}


