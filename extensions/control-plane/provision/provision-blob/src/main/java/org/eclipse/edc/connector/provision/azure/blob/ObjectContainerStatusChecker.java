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

package org.eclipse.edc.connector.provision.azure.blob;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.StatusChecker;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.EdcException;

import java.util.List;

import static java.lang.String.format;

public class ObjectContainerStatusChecker implements StatusChecker {
    private final BlobStoreApi blobStoreApi;
    private final RetryPolicy<Object> retryPolicy;

    public ObjectContainerStatusChecker(BlobStoreApi blobStoreApi, RetryPolicy<Object> retryPolicy) {
        this.blobStoreApi = blobStoreApi;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources) {
        if (!resources.isEmpty()) {
            for (var resource : resources) {
                if (resource instanceof ObjectContainerProvisionedResource) {
                    var provisionedResource = (ObjectContainerProvisionedResource) resource;
                    String accountName = provisionedResource.getAccountName();
                    String containerName = provisionedResource.getContainerName();
                    return checkContainerExists(accountName, containerName);
                }
            }
        } else {
            var accountName = transferProcess.getDataRequest().getDataDestination().getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
            var containerName = transferProcess.getDataRequest().getDataDestination().getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME);
            return checkContainerExists(accountName, containerName);
        }
        throw new EdcException(format("No object container resource was associated with the transfer process: %s - cannot determine completion.", transferProcess));
    }

    private boolean checkContainerExists(String accountName, String containerName) {
        if (!blobStoreApi.exists(accountName, containerName)) {
            return false;
        }

        // TODO: this checks if there is **any** file ends with *.complete. This can be improved to check for a particular file name, if specified
        return Failsafe.with(retryPolicy).get(() -> blobStoreApi.listContainer(accountName, containerName)
                .stream().anyMatch(bci -> bci.getName().endsWith(".complete")));
    }

}
