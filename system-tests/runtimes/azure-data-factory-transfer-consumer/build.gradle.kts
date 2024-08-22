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
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.core.connector)
    implementation(libs.edc.core.controlplane)
    implementation(libs.edc.dpf.selector.core)
    implementation(libs.edc.http)

    implementation(libs.edc.api.observability)

    implementation(libs.edc.config.filesystem)
    implementation(libs.edc.iam.mock)

    implementation(libs.edc.api.management)

    implementation(libs.edc.dsp)

    implementation(project(":extensions:control-plane:provision:provision-blob"))
    implementation(project(":extensions:common:vault:vault-azure"))
    implementation(libs.edc.lib.util)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("consumer.jar")
}

edcBuild {
    publish.set(false)
}
