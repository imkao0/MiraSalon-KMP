plugins {
    `kotlin-dsl`
}

group = "mirasalon.buildlogic"

dependencies {
    implementation(libs.detekt.gradle)
    implementation(libs.ktlint.gradle)
}
