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
    testFixturesApi(libs.edc.spi.transaction.datasource)
    testFixturesApi(libs.postgres)
    testFixturesApi(libs.testcontainers.junit)
    testFixturesApi(libs.edc.lib.util)
    testFixturesApi(libs.edc.junit)
    testFixturesApi(libs.edc.sql.lib)
    testFixturesApi(libs.azure.storageblob)
}


