plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.dpf.selector.spi)

    implementation(project(":extensions:common:azure:azure-cosmos-core"))
    implementation(libs.azure.cosmos)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(libs.edc.dpf.selector.spi))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}


