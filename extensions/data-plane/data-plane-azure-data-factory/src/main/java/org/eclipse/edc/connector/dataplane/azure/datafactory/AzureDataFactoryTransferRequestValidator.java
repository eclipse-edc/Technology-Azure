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

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateBlobName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateKeyName;

/**
 * Validator for {@link AzureDataFactoryTransferService}.
 */
public class AzureDataFactoryTransferRequestValidator {

    /**
     * Returns true if this service can transfer the request.
     */
    boolean canHandle(DataFlowStartMessage request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getSourceDataAddress().getType()) &&
                AzureBlobStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    /**
     * Returns true if the request is valid.
     */
    @NotNull Result<Boolean> validate(DataFlowStartMessage request) {
        try {
            validateSource(request.getSourceDataAddress());
            validateDestination(request.getDestinationDataAddress());
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
        return Result.success(true);
    }

    private void validateSource(DataAddress source) {
        validateBlobName(source.getStringProperty(AzureBlobStoreSchema.BLOB_NAME));
        validateCommon(source);
    }

    private void validateDestination(DataAddress destination) {
        validateCommon(destination);
    }

    private void validateCommon(DataAddress dataAddress) {
        validateAccountName(dataAddress.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME));
        validateContainerName(dataAddress.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME));
        validateKeyName(dataAddress.getKeyName());
    }
}
