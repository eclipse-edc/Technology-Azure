/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    testImplementation(libs.edc.ih.spi.store)

    testImplementation(testFixtures(project(":extensions:common:azure:azure-test")))
    testImplementation(project(":extensions:common:azure:azure-test"))
    testImplementation(libs.edc.sql.lib)
    testImplementation(libs.edc.sql.identityhub.store)
    testImplementation(testFixtures(libs.edc.ih.spi.store))

}
