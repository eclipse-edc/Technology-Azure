/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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
    api(libs.edc.spi.contract)
    api(libs.edc.lib.util)

    implementation(libs.failsafe.core)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(libs.awaitility)

    testImplementation(libs.edc.sql.core)
    testImplementation(libs.edc.sql.contractnegotiation)
    testImplementation(testFixtures(libs.edc.sql.lease))
    testImplementation(testFixtures(libs.edc.spi.contract))

}


