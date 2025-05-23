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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedDataDestinationResource;

import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.FOLDER_NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@JsonDeserialize(builder = ObjectContainerProvisionedResource.Builder.class)
@JsonTypeName("dataspaceconnector:objectcontainerprovisionedresource")
public class ObjectContainerProvisionedResource extends ProvisionedDataDestinationResource {

    private ObjectContainerProvisionedResource() {
    }

    public String getAccountName() {
        return getDataAddress().getStringProperty(ACCOUNT_NAME);
    }

    public String getContainerName() {
        return getDataAddress().getStringProperty(CONTAINER_NAME);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedDataDestinationResource.Builder<ObjectContainerProvisionedResource, Builder> {

        private Builder() {
            super(new ObjectContainerProvisionedResource());
            dataAddressBuilder.type(AzureBlobStoreSchema.TYPE);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            dataAddressBuilder.property(EDC_NAMESPACE + ACCOUNT_NAME, accountName);
            return this;
        }

        public Builder containerName(String containerName) {
            dataAddressBuilder.property(EDC_NAMESPACE + CONTAINER_NAME, containerName);
            return this;
        }

        @Override
        public Builder resourceName(String name) {
            dataAddressBuilder.keyName(name);
            super.resourceName(name);
            return this;
        }

        public Builder folderName(String folderName) {
            if (folderName != null) {
                dataAddressBuilder.property(EDC_NAMESPACE + FOLDER_NAME, folderName);
            }
            return this;
        }

        public Builder blobName(String blobName) {
            if (blobName != null) {
                dataAddressBuilder.property(EDC_NAMESPACE + BLOB_NAME, blobName);
            }
            return this;
        }
    }
}
