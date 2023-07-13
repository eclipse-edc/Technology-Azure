plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(libs.edc.dpf.selector.spi)

    implementation(libs.failsafe.core)

    testImplementation(libs.edc.sql.core)
    testImplementation(libs.edc.sql.dataplane.instancestore)
    testImplementation(testFixtures(libs.edc.dpf.selector.spi))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}


