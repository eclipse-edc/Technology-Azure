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

package org.eclipse.edc.connector.dataplane.provision.azure;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.provision.azure.blob.ObjectStorageConsumerProvisionResourceGenerator;
import org.eclipse.edc.connector.dataplane.provision.azure.blob.ObjectStorageDeprovisioner;
import org.eclipse.edc.connector.dataplane.provision.azure.blob.ObjectStorageProvisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Provides data transfer provisioners backed by Azure services.
 */
public class AzureProvisionExtension implements ServiceExtension {

    @Configuration
    private AzureProvisionConfiguration azureProvisionConfiguration;

    @Inject
    private BlobStoreApi blobStoreApi;

    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private ResourceDefinitionGeneratorManager manifestGenerator;

    @Inject
    private TypeManager typeManager;

    @Inject
    private ProvisionerManager provisionManager;

    @Inject
    private Monitor monitor;

    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return "Dataplane Azure Provisioner";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        provisionManager.register(new ObjectStorageProvisioner(retryPolicy, context.getMonitor(), blobStoreApi, azureProvisionConfiguration, typeManager, participantContextSupplier));
        provisionManager.register(new ObjectStorageDeprovisioner());
        manifestGenerator.registerConsumerGenerator(new ObjectStorageConsumerProvisionResourceGenerator(monitor.withPrefix("AzureStorageProvisioner")));

        registerTypes(typeManager);
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(AzureSasToken.class);
    }

}
