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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added dependencies
 *
 */

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.edc.controlplane.base.bom) {
        exclude("org.eclipse.edc", "data-plane-signaling")
        exclude("org.eclipse.edc", "data-plane-signaling-oauth2")
    }

    implementation(libs.edc.iam.mock)

    implementation(libs.edc.core.dataplane)
    implementation(libs.edc.transfer.data.plane.signaling)
    implementation(libs.edc.data.plane.signaling.client)
    implementation(libs.edc.data.plane.self.registration)
    implementation(project(":extensions:data-plane:data-plane-provision-blob"))

    implementation(libs.edc.lib.util)
    implementation(libs.edc.core.controlplane.apiclient)
    implementation(libs.edc.transaction.local)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.shadowJar {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("consumer.jar")
}

edcBuild {
    publish.set(false)
}
