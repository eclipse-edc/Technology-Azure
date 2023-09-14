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
    `java-test-fixtures`
    `maven-publish`
}

dependencies {
    api(libs.edc.controlplane.spi)
    testFixturesApi(libs.postgres)
    testFixturesApi(libs.testcontainers.junit)

    testFixturesApi(libs.edc.util)
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.edc.sql.core)
    testFixturesApi(libs.azure.storageblob)
    testFixturesApi(libs.junit.jupiter.api)
}


