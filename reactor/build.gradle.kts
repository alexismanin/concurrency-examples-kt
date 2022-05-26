plugins {
    `kotlin-convention`
}

dependencies {
    implementation(project(":common"))

    implementation(platform("io.projectreactor:reactor-bom:2020.0.19"))

    implementation("io.projectreactor:reactor-core")
}