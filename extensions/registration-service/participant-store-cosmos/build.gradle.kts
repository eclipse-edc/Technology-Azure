/*
 *  Copyright (c) 2020, 2022 Microsoft Corporation
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
    testRuntimeOnly(libs.edc.rs.spi.store)
    testImplementation(project(":extensions:common:azure:azure-test"))
    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(libs.edc.sql.core)
    testImplementation(libs.edc.sql.participant.store)
    testImplementation(testFixtures(libs.edc.rs.spi.store))
    testImplementation(testFixtures(libs.edc.ext.azure.test))

}

