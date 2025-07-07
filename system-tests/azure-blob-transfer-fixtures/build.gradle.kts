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
    `java-test-fixtures`
}

dependencies {
    testFixturesApi(project(":extensions:common:azure:azure-blob-core"))
    testFixturesApi(libs.edc.jsonld)
    testFixturesApi(libs.edc.lib.util)
    testFixturesApi(libs.edc.spi.core)
    testFixturesApi(testFixtures(libs.edc.management.api.test.fixtures))
    testFixturesImplementation(libs.restAssured)
    testFixturesImplementation(libs.azure.storageblob)
    testFixturesImplementation(libs.edc.junit)
}

edcBuild {
    publish.set(false)
}
