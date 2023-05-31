/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.core.dataplane)
    implementation(libs.edc.dpf.http)
    implementation(libs.edc.dpf.http.oauth2)
    implementation(libs.edc.dpf.api)
    implementation(libs.edc.vault.filesystem)
}

edcBuild {
    publish.set(false)
}
