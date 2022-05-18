plugins {
    `kotlin-convention`
}

dependencies {
    implementation(platform("io.projectreactor:reactor-bom:2020.0.19"))

    implementation("io.projectreactor:reactor-core")
}