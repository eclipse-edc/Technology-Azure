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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *       ZF Friedrichshafen AG - add dependency & reorder entries
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

rootProject.name = "technology-azure"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// core extensions
include(":extensions:common:azure:azure-eventgrid")
include(":extensions:common:azure:azure-resource-manager")
include(":extensions:common:azure:azure-test")
include(":extensions:common:azure:azure-blob-core")
include(":extensions:common:vault:vault-azure")

// controlplane extensions

include(":extensions:control-plane:store:asset-index-cosmos")
include(":extensions:control-plane:store:contract-definition-store-cosmos")
include(":extensions:control-plane:store:contract-negotiation-store-cosmos")
include(":extensions:control-plane:store:policy-definition-store-cosmos")
include(":extensions:control-plane:store:transfer-process-store-cosmos")
include(":extensions:control-plane:provision:provision-blob")

// dataplane extensions
include(":extensions:data-plane:data-plane-azure-storage")
include(":extensions:data-plane:store:data-plane-store-cosmos")
include(":extensions:data-plane:data-plane-azure-data-factory")
include(":extensions:data-plane-selector:data-plane-instance-store-cosmos")

// system test modules
include(":system-tests:azure-blob-transfer-tests")
include(":system-tests:azure-data-factory-tests")
include(":system-tests:runtimes:azure-data-factory-transfer-consumer")
include(":system-tests:runtimes:azure-data-factory-transfer-provider")
include(":system-tests:runtimes:azure-storage-transfer-consumer")
include(":system-tests:runtimes:azure-storage-transfer-provider")
include(":system-tests:azure-blob-transfer-fixtures")
