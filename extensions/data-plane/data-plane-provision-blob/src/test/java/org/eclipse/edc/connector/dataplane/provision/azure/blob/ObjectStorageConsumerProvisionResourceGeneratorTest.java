/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.dataplane.provision.azure.blob;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class ObjectStorageConsumerProvisionResourceGeneratorTest {

    private final ObjectStorageConsumerProvisionResourceGenerator generator =
            new ObjectStorageConsumerProvisionResourceGenerator(mock(Monitor.class));

    @Test
    void generate() {
        var destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(EDC_NAMESPACE + AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                .property(EDC_NAMESPACE + AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                .property(EDC_NAMESPACE + AzureBlobStoreSchema.BLOB_NAME, "test-blob")
                .property(EDC_NAMESPACE + AzureBlobStoreSchema.FOLDER_NAME, "test-folder")
                .build();

        var dataflow = DataFlow.Builder.newInstance()
                .id("flow-id")
                .destination(destination)
                .build();

        var resource = generator.generate(dataflow);

        assertThat(resource).isInstanceOf(ProvisionResource.class);
        assertThat(resource.getFlowId()).isEqualTo("flow-id");
        assertThat(resource.getDataAddress()).isNotNull();
        assertThat(resource.getDataAddress().getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME)).isEqualTo("test-account");
        assertThat(resource.getDataAddress().getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME)).isEqualTo("test-container");
        assertThat(resource.getDataAddress().getStringProperty(AzureBlobStoreSchema.BLOB_NAME)).isEqualTo("test-blob");
        assertThat(resource.getDataAddress().getStringProperty(AzureBlobStoreSchema.FOLDER_NAME)).isEqualTo("test-folder");
    }

    @Test
    void shouldProvisionRandomUuids_whenDestinationIsNull() {
        var dataflow = DataFlow.Builder.newInstance()
                .id("flow-id")
                .destination(null)
                .build();

        var resource = generator.generate(dataflow);

        assertThat(resource).isInstanceOfSatisfying(ProvisionResource.class, def -> {
            assertThat(resource.getDataAddress().getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME)).satisfies(UUID::fromString);
            assertThat(resource.getDataAddress().getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME)).satisfies(UUID::fromString);
            assertThat(def.getId()).satisfies(UUID::fromString);
        });

    }


}
