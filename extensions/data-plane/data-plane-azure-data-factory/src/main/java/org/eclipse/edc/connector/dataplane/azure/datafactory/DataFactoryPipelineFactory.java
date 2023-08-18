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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import com.azure.resourcemanager.datafactory.models.AzureBlobStorageLocation;
import com.azure.resourcemanager.datafactory.models.AzureKeyVaultSecretReference;
import com.azure.resourcemanager.datafactory.models.AzureStorageLinkedService;
import com.azure.resourcemanager.datafactory.models.BinaryDataset;
import com.azure.resourcemanager.datafactory.models.BlobSink;
import com.azure.resourcemanager.datafactory.models.BlobSource;
import com.azure.resourcemanager.datafactory.models.CopyActivity;
import com.azure.resourcemanager.datafactory.models.DatasetReference;
import com.azure.resourcemanager.datafactory.models.DatasetResource;
import com.azure.resourcemanager.datafactory.models.LinkedServiceReference;
import com.azure.resourcemanager.datafactory.models.LinkedServiceResource;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

/**
 * Factory class for Azure Data Factory object definitions, such as pipelines and datasets.
 */
class DataFactoryPipelineFactory {
    private static final String ADF_RESOURCE_NAME_PREFIX = "EDC-DPF-";
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net/";

    private final String keyVaultLinkedService;
    private final KeyVaultClient keyVaultClient;
    private final DataFactoryClient client;
    private final TypeManager typeManager;

    DataFactoryPipelineFactory(String keyVaultLinkedService, KeyVaultClient keyVaultClient, DataFactoryClient client, TypeManager typeManager) {
        this.keyVaultLinkedService = keyVaultLinkedService;
        this.keyVaultClient = keyVaultClient;
        this.client = client;
        this.typeManager = typeManager;
    }

    /**
     * Create a Data Factory pipeline for a transfer request.
     *
     * @param request the transfer request.
     * @return the created pipeline resource.
     */
    PipelineResource createPipeline(DataFlowRequest request) {
        var baseName = ADF_RESOURCE_NAME_PREFIX + UUID.randomUUID();

        var sourceDataset = createSourceDataset(baseName + "-src", request.getSourceDataAddress());
        var destinationDataset = createDestinationDataset(baseName + "-dst", request.getDestinationDataAddress());

        return createCopyPipeline(baseName, sourceDataset, destinationDataset);
    }

    private PipelineResource createCopyPipeline(String baseName, DatasetResource sourceDataset, DatasetResource destinationDataset) {
        return client.definePipeline(baseName)
                .withActivities(List.of(new CopyActivity()
                        .withName("CopyActivity")
                        .withInputs(List.of(new DatasetReference().withReferenceName(sourceDataset.name())))
                        .withOutputs(List.of(new DatasetReference().withReferenceName(destinationDataset.name())))
                        .withSource(new BlobSource())
                        .withSink(new BlobSink())
                        .withValidateDataConsistency(false)))
                .create();
    }

    private DatasetResource createSourceDataset(String name, DataAddress sourceDataAddress) {
        var linkedService = createSourceLinkedService(name, sourceDataAddress);
        return createDatasetResource(name, linkedService, sourceDataAddress);
    }

    private DatasetResource createDestinationDataset(String name, DataAddress sourceDataAddress) {
        var linkedService = createDestinationLinkedService(name, sourceDataAddress);
        return createDatasetResource(name, linkedService, sourceDataAddress);
    }

    private LinkedServiceResource createSourceLinkedService(String name, DataAddress dataAddress) {
        var accountName = dataAddress.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME);

        return client.defineLinkedService(name)
                .withProperties(new AzureStorageLinkedService()
                        .withConnectionString(String.format("DefaultEndpointsProtocol=https;AccountName=%s;", accountName))
                        .withAccountKey(
                                new AzureKeyVaultSecretReference()
                                        .withSecretName(dataAddress.getKeyName())
                                        .withStore(new LinkedServiceReference()
                                                .withReferenceName(keyVaultLinkedService)
                                        ))
                )
                .create();
    }

    private LinkedServiceResource createDestinationLinkedService(String name, DataAddress dataAddress) {
        var accountName = dataAddress.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var secret = keyVaultClient.getSecret(dataAddress.getKeyName());
        var token = typeManager.readValue(secret.getValue(), AzureSasToken.class);
        var sasTokenSecret = keyVaultClient.setSecret(name, token.getSas());

        return client.defineLinkedService(name)
                .withProperties(
                        new AzureStorageLinkedService()
                                .withSasUri(format(BLOB_STORE_ENDPOINT_TEMPLATE, accountName))
                                .withSasToken(
                                        new AzureKeyVaultSecretReference()
                                                .withSecretName(sasTokenSecret.getName())
                                                .withStore(new LinkedServiceReference()
                                                        .withReferenceName(keyVaultLinkedService)
                                                ))
                )
                .create();
    }

    private DatasetResource createDatasetResource(String name, LinkedServiceResource linkedService, DataAddress dataAddress) {
        return client.defineDataset(name)
                .withProperties(
                        new BinaryDataset()
                                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(linkedService.name()))
                                .withLocation(new AzureBlobStorageLocation()
                                        .withFileName(dataAddress.getStringProperty(AzureBlobStoreSchema.BLOB_NAME))
                                        .withContainer(dataAddress.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                                )
                )
                .create();
    }
}
