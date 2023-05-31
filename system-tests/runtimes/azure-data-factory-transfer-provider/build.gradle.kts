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
 *       Fraunhofer Institute for Software and Systems Engineering - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.spi.dataplane)
    implementation(libs.edc.util)
    implementation(libs.edc.core.controlplane)
    implementation(libs.edc.dpf.selector.core)
    implementation(libs.edc.core.dataplane)
    implementation(libs.edc.transfer.dataplane)
    implementation(libs.edc.dpf.client)
    implementation(libs.edc.dpf.selector.client)
    implementation(libs.edc.dpf.selector.core)
    implementation(project(":extensions:data-plane:data-plane-azure-data-factory"))
    implementation(project(":extensions:common:azure:azure-resource-manager"))
    implementation(libs.edc.api.observability)
    implementation(libs.edc.config.filesystem)
    implementation(libs.edc.iam.mock)
    implementation(libs.edc.api.management)
    implementation(project(":extensions:control-plane:provision:provision-blob"))
    implementation(project(":extensions:common:vault:vault-azure"))
    implementation(libs.edc.dsp)

    implementation(libs.jakarta.rsApi)
    implementation(libs.azure.identity)
    implementation(libs.azure.resourcemanager.datafactory)
    implementation(libs.azure.resourcemanager.storage)
    implementation(libs.azure.resourcemanager.keyvault)
    implementation(libs.azure.resourcemanager)
    implementation(libs.azure.resourcemanager.authorization)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("provider.jar")
}

edcBuild {
    publish.set(false)
}
