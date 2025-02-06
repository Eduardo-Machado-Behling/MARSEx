plugins {
    java
    application
}

repositories {
    mavenCentral() // jcenter() is deprecated; mavenCentral() is the preferred alternative
    maven {
            url = uri("https://jogamp.org/deployment/maven/")
    }
}

val joglVersion = "2.4.0" // Define a variable for the version

tasks.register<JavaExec>("runWithDebug") {
    mainClass.set("Mars")  // Replace with your main class
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf(
        "-Xdebug",
        "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
    )
}

dependencies {
    implementation("org.jogamp.gluegen:gluegen-rt:$joglVersion")
    implementation("org.jogamp.jogl:jogl-all:$joglVersion")

    // Native dependencies (using platform-specific configurations)
    val nativePlatforms = listOf(
        "android-aarch64",
        "linux-amd64",
        "macosx-universal",
        "windows-amd64",
    )

    nativePlatforms.forEach { platform ->
        runtimeOnly("org.jogamp.gluegen:gluegen-rt:$joglVersion:natives-$platform")
        runtimeOnly("org.jogamp.jogl:jogl-all:$joglVersion:natives-$platform")
    }
}

application {
    mainClass.set("Mars")
}

// Create a fat JAR (including all dependencies)
tasks.jar {
archiveClassifier.set("")

    manifest {
        attributes(mapOf("Main-Class" to "Mars"))
    }

    // This is the KEY change: Handle duplicates
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Or DuplicatesStrategy.WARN if you want to see warnings

    from({
        configurations.runtimeClasspath.get().files.map {
            if (it.isDirectory) it else zipTree(it)
        }
    })

    // Include native libraries explicitly (if they are not already in jogl-all)
    // If they *are* in jogl-all, uncommenting this will probably cause errors.
    // from({
    //     configurations.runtimeClasspath.get().files
    //         .filter { it.name.contains("natives"))
    //         .map { if (it.isDirectory) it else zipTree(it) }
    // }) {
    //     into("lib/natives")  // Or into("natives") if you don't want the lib folder
    // }

}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

