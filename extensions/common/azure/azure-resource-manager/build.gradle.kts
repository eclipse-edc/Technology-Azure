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
    implementation(libs.edc.core.dataPlane.util)
    implementation(libs.edc.lib.util)
    implementation(libs.azure.identity)
    implementation(libs.azure.resourcemanager)
    implementation(libs.azure.resourcemanager.authorization)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))

    testImplementation(libs.edc.junit)
}


