plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":adapter-scenario"))
    implementation(project(":adapter-storage"))
    testImplementation(kotlin("test"))
}

application {
    mainClass = "trap.towers.cli.MainKt"
}
