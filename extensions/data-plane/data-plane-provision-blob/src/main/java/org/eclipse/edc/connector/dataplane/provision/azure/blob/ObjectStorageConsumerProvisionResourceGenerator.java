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
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.string.StringUtils.isNullOrEmpty;


public class ObjectStorageConsumerProvisionResourceGenerator implements ResourceDefinitionGenerator {

    private final Monitor monitor;

    public ObjectStorageConsumerProvisionResourceGenerator(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public String supportedType() {
        return AzureBlobStoreSchema.TYPE;
    }

    @Override
    public ProvisionResource generate(DataFlow dataFlow) {

        var originalDataDestination = dataFlow.getDestination();
        var toProvision = DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .property(EDC_NAMESPACE + AzureBlobStoreSchema.ACCOUNT_NAME, UUID.randomUUID().toString())
                .property(EDC_NAMESPACE + AzureBlobStoreSchema.CONTAINER_NAME, UUID.randomUUID().toString());

        if (originalDataDestination != null) {
            toProvision.properties(originalDataDestination.getProperties());
            if (isNullOrEmpty(originalDataDestination.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME))) {
                monitor.debug("Container name on destination is null or empty. A random one will be used during provisioning.");
            }
            if (isNullOrEmpty(originalDataDestination.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME))) {
                monitor.debug("Account name on destination is null or empty. A random one will be used during provisioning.");
            }
        }

        return ProvisionResource.Builder.newInstance()
                .flowId(dataFlow.getId())
                .type(AzureBlobStoreSchema.TYPE)
                .dataAddress(toProvision.build())
                .build();
    }
}
