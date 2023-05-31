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
    implementation(libs.edc.core.jwt)
    implementation(libs.edc.core.controlplane)
    implementation(libs.edc.dsp)
    implementation(libs.edc.ext.http)
    implementation(libs.edc.iam.mock)
    implementation(libs.edc.api.management)
    implementation(libs.edc.transfer.dataplane)
    implementation(libs.edc.dpf.client)
    implementation(libs.edc.vault.filesystem)

    implementation(libs.edc.dpf.selector.core)
    implementation(libs.edc.dpf.selector.api)
    implementation(libs.edc.dpf.selector.client)

    implementation(libs.edc.provision.http)
    implementation(libs.edc.transfer.httppull.receiver)
    implementation(libs.edc.transfer.httppull.receiver.dynamic)

}

edcBuild {
    publish.set(false)
}
