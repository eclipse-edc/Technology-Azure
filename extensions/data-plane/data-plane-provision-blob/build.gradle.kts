/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */


plugins {
    `java-library`
}

dependencies {
    api(libs.edc.spi.core)
    api(libs.edc.spi.dataplane)
    api(libs.edc.spi.participant.context.single)

    implementation(libs.edc.lib.util)
    implementation(project(":extensions:common:azure:azure-blob-core"))
    implementation(libs.azure.storageblob)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
}


