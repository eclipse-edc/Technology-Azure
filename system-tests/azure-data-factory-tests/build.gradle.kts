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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

plugins {
    `java-library`
}

dependencies {

    testImplementation(project(":extensions:common:azure:azure-blob-core"))
    testImplementation(project(":extensions:common:vault:vault-azure"))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(testFixtures(project(":system-tests:azure-blob-transfer-fixtures")))

    testImplementation(libs.edc.junit)
    testImplementation(libs.azure.storageblob)
    testImplementation(libs.restAssured)
    testImplementation(libs.azure.identity)
    testImplementation(libs.azure.keyvault)

    testRuntimeOnly(project(":system-tests:runtimes:azure-data-factory-transfer-provider"))
    testRuntimeOnly(project(":system-tests:runtimes:azure-data-factory-transfer-consumer"))
}

edcBuild {
    publish.set(false)
}
