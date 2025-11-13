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
    implementation(libs.edc.core.runtime)
    implementation(libs.edc.core.connector)
    implementation(libs.edc.lib.util)
    implementation(libs.edc.http)

    implementation(libs.edc.core.controlplane)
    implementation(libs.edc.dpf.selector.core)
    implementation(libs.edc.core.dataplane)
    implementation(libs.edc.core.edrstore)

    implementation(libs.edc.core.participantcontext.single)
    implementation(libs.edc.transfer.data.plane.signaling)
    implementation(libs.edc.data.plane.signaling.client)
    implementation(libs.edc.data.plane.self.registration)
    implementation(libs.edc.dpf.selector.core)
    implementation(project(":extensions:data-plane:data-plane-azure-storage"))

    implementation(libs.edc.spi.dataplane)

    implementation(libs.edc.api.observability)
    implementation(libs.edc.api.control.config)

    implementation(libs.edc.config.filesystem)
    implementation(libs.edc.iam.mock)
    implementation(libs.edc.api.management)

    implementation(libs.edc.dsp)
    implementation(libs.edc.core.controlplane.apiclient)
    implementation(libs.edc.core.controlplane.api)
    implementation(libs.edc.transaction.local)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.shadowJar {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("provider.jar")
}

edcBuild {
    publish.set(false)
}
