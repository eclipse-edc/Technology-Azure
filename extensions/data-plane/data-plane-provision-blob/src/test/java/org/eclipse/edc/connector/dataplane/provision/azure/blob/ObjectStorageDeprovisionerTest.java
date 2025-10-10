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
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class ObjectStorageDeprovisionerTest {

    private ObjectStorageDeprovisioner deprovisioner;

    @BeforeEach
    public void setUp() {
        deprovisioner = new ObjectStorageDeprovisioner();
    }

    @Test
    public void deprovision_should_not_do_anything() {
        var resource = createProvisionedResource();
        var result = deprovisioner.deprovision(resource);

        assertThat(result).succeedsWithin(1, SECONDS);
    }

    private ProvisionResource createProvisionedResource() {
        return ProvisionResource.Builder.newInstance()
                .dataAddress(DataAddress.Builder.newInstance()
                        .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account-name")
                        .property(AzureBlobStoreSchema.CONTAINER_NAME, "test-container-name")
                        .type(AzureBlobStoreSchema.TYPE)
                        .build())
                .flowId("some-flow-id")
                .type(AzureBlobStoreSchema.TYPE)
                .build();
    }


}
