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

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.util.string.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_PREFIX;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateKeyName;

/**
 * Instantiates {@link AzureStorageDataSink}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSinkFactory implements DataSinkFactory {
    private final BlobStoreApi blobStoreApi;
    private final ExecutorService executorService;
    private final int partitionSize;
    private final Monitor monitor;
    private final Vault vault;
    private final TypeManager typeManager;
    private final BlobMetadataProvider metadataProvider;

    public AzureStorageDataSinkFactory(BlobStoreApi blobStoreApi, ExecutorService executorService, int partitionSize, Monitor monitor, Vault vault, TypeManager typeManager, BlobMetadataProvider metadataProvider) {
        this.blobStoreApi = blobStoreApi;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public boolean canHandle(DataFlowStartMessage request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public DataSink createSink(DataFlowStartMessage request) {
        var validate = validateRequest(request);
        if (validate.failed()) {
            throw new EdcException(validate.getFailure().getMessages().toString());
        }

        var dataAddress = request.getDestinationDataAddress();
        var dataSourceAddress = request.getSourceDataAddress();
        var requestId = request.getId();

        var secret = vault.resolveSecret(dataAddress.getKeyName());
        var token = typeManager.readValue(secret, AzureSasToken.class);

        return AzureStorageDataSink.Builder.newInstance()
                .accountName(dataAddress.getStringProperty(ACCOUNT_NAME))
                .containerName(dataAddress.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                .folderName(dataAddress.getStringProperty(AzureBlobStoreSchema.FOLDER_NAME))
                .blobName(dataAddress.getStringProperty(AzureBlobStoreSchema.BLOB_NAME))
                .blobPrefix(dataSourceAddress.getStringProperty(BLOB_PREFIX))
                .sharedAccessSignature(token.getSas())
                .requestId(requestId)
                .partitionSize(partitionSize)
                .blobStoreApi(blobStoreApi)
                .executorService(executorService)
                .monitor(monitor)
                .request(request)
                .metadataProvider(metadataProvider)
                .build();
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        var dataAddress = request.getDestinationDataAddress();
        var dataSourceAddress = request.getSourceDataAddress();

        try {
            validateAccountName(dataAddress.getStringProperty(ACCOUNT_NAME));
            validateContainerName(dataAddress.getStringProperty(CONTAINER_NAME));
            validateKeyName(dataAddress.getKeyName());
            if (dataSourceAddress.hasProperty(BLOB_PREFIX)) {
                if (!StringUtils.isNullOrBlank(BLOB_NAME)) {
                    monitor.warning(String.format("Folder transfer, ignoring property %s", BLOB_NAME));
                }
            }
        } catch (IllegalArgumentException e) {
            return Result.failure("AzureStorage destination address is invalid: " + e.getMessage());
        }
        return Result.success();
    }
}
