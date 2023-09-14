plugins {
    `java-library`
    `maven-publish`
}


dependencies {
    api(libs.edc.spi.dataplane)

    implementation(libs.failsafe.core)

    testImplementation(libs.edc.sql.core)
    testImplementation(libs.edc.sql.dataplane.store)
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(libs.edc.spi.dataplane))
    testImplementation(testFixtures(libs.edc.dpf.selector.spi))
    testImplementation(testFixtures(libs.edc.sql.lease))
    testImplementation(libs.awaitility)

}


