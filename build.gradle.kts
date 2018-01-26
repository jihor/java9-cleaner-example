plugins {
    application
}

application {
    mainClassName = "ru.jihor.java9-cleaner-example"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

dependencies {
    testCompile("junit:junit:4.12")
}

repositories {
    jcenter()
}
