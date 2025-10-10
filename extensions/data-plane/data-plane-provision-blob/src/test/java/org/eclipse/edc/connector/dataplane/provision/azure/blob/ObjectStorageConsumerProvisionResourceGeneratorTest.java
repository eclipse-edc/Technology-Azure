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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageConsumerProvisionResourceGeneratorTest {

    private final ObjectStorageConsumerProvisionResourceGenerator generator =
            new ObjectStorageConsumerProvisionResourceGenerator();

    @Test
    void generate() {
        var destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
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
    }


}
