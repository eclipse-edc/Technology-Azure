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
 *
 */

package org.eclipse.edc.connector.provision.azure;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.provision.azure.blob.ObjectContainerProvisionedResource;
import org.eclipse.edc.connector.provision.azure.blob.ObjectStorageConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.provision.azure.blob.ObjectStorageProvisioner;
import org.eclipse.edc.connector.provision.azure.blob.ObjectStorageResourceDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Provides data transfer {@link Provisioner}s backed by Azure services.
 */
public class AzureProvisionExtension implements ServiceExtension {

    @Configuration
    private AzureProvisionConfiguration azureProvisionConfiguration;

    @Inject
    private BlobStoreApi blobStoreApi;

    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private TypeManager typeManager;

    @Inject
    private TransferTypeParser transferTypeParser;

    @Inject
    private ProvisionManager provisionManager;

    @Override
    public String name() {
        return "Azure Provision";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        provisionManager.register(new ObjectStorageProvisioner(retryPolicy, context.getMonitor(), blobStoreApi, azureProvisionConfiguration));
        manifestGenerator.registerGenerator(new ObjectStorageConsumerResourceDefinitionGenerator(transferTypeParser));

        registerTypes(typeManager);
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(ObjectContainerProvisionedResource.class, ObjectStorageResourceDefinition.class, AzureSasToken.class);
    }

}
