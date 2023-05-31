plugins {
    `java-library`
    `maven-publish`
}


dependencies {
    api(libs.edc.spi.dataplane)
    api(project(":extensions:common:azure:azure-cosmos-core"))

    implementation(libs.azure.cosmos)
    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(libs.edc.spi.dataplane))

}


