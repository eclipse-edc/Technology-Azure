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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.Map;

import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createDataAddress;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;

public class TestFunctions {

    public static Map<String, Object> sourceProperties() {
        var srcStorageAccount = createAccountName();
        return DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .keyName(srcStorageAccount + "-key1")
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, srcStorageAccount)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, createContainerName())
                .property(AzureBlobStoreSchema.BLOB_NAME, createBlobName())
                .build()
                .getProperties();
    }

    public static Map<String, Object> destinationProperties() {
        var destStorageAccount = createAccountName();

        return DataAddress.Builder.newInstance()
                .type(AzureBlobStoreSchema.TYPE)
                .keyName(destStorageAccount + "-key1")
                .property(AzureBlobStoreSchema.ACCOUNT_NAME, destStorageAccount)
                .property(AzureBlobStoreSchema.CONTAINER_NAME, createContainerName())
                .build()
                .getProperties();
    }

    public static DataFlowRequest createFlowRequest() {
        return createRequest(AzureBlobStoreSchema.TYPE)
                .sourceDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(sourceProperties()).build())
                .destinationDataAddress(createDataAddress(AzureBlobStoreSchema.TYPE).properties(destinationProperties()).build())
                .build();
    }
}
