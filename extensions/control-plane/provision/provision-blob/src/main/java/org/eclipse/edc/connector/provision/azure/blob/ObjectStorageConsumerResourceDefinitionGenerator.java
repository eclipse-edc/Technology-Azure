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
 *       ZF Friedrichshafen AG - improvements (refactoring of generate method)
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.provision.azure.blob;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class ObjectStorageConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    private final TransferTypeParser transferTypeParser;

    public ObjectStorageConsumerResourceDefinitionGenerator(TransferTypeParser transferTypeParser) {
        this.transferTypeParser = transferTypeParser;
    }

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess transferProcess, Policy policy) {
        var definitionBuilder = ObjectStorageResourceDefinition.Builder.newInstance()
                .id(randomUUID().toString())
                .containerName(randomUUID().toString())
                .accountName(randomUUID().toString());

        var destination = transferProcess.getDataDestination();
        if (destination != null) {
            definitionBuilder
                    .accountName(destination.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME))
                    .containerName(destination.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME, randomUUID().toString()))
                    .folderName(destination.getStringProperty(AzureBlobStoreSchema.FOLDER_NAME));
        }

        return definitionBuilder.build();
    }

    @Override
    public boolean canGenerate(TransferProcess transferProcess, Policy policy) {
        return transferTypeParser.parse(transferProcess.getTransferType())
                .map(TransferType::destinationType)
                .map(AzureBlobStoreSchema.TYPE::equals)
                .orElse(failure -> false);
    }
}
