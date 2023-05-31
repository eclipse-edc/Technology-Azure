/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    api(project(":extensions:common:azure:azure-blob-core"))
    api(libs.edc.spi.dataplane)
    implementation(libs.edc.core.dataPlane.util)
    implementation(libs.edc.util)
    implementation(libs.azure.identity)
    implementation(libs.azure.resourcemanager.datafactory)
    implementation(libs.azure.resourcemanager.storage)
    implementation(libs.azure.resourcemanager.keyvault)
    implementation(libs.azure.resourcemanager)
    implementation(libs.azure.resourcemanager.authorization)

    testImplementation(libs.edc.config.filesystem)
    testImplementation(libs.edc.core.dataplane)
    testImplementation(project(":extensions:common:azure:azure-resource-manager"))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-blob-core")))

    testImplementation(libs.edc.junit)
    testImplementation(libs.awaitility)
    testImplementation(libs.bouncyCastle.bcprovJdk18on)
}


