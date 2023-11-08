/*
 *  Copyright (c) 2023 Robert Bosch GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Robert Bosch GmbH - adaption for blob metadata
 *
 */

package org.eclipse.edc.test.system.blob;

import jakarta.json.JsonObject;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.util.string.StringUtils;

import java.util.Objects;

import static jakarta.json.Json.createObjectBuilder;

public class TransferDestination {

    private final String accountName;
    private final String containerName;
    private final String correlationId;
    private final String folderName;
    private final String keyName;

    private TransferDestination(Builder builder) {
        this.accountName = builder.accountName;
        this.containerName = builder.containerName;
        this.correlationId = builder.correlationId;
        this.folderName = builder.folderName;
        this.keyName = builder.keyName;
    }

    public JsonObject toJsonObject() {
        var properties = createObjectBuilder()
                .add(AzureBlobStoreSchema.ACCOUNT_NAME, accountName);
        if (!StringUtils.isNullOrEmpty(containerName)) {
            properties.add(AzureBlobStoreSchema.CONTAINER_NAME, containerName);
        }
        if (!StringUtils.isNullOrEmpty(correlationId)) {
            properties.add(AzureBlobStoreSchema.CORRELATION_ID, correlationId);
        }
        if (!StringUtils.isNullOrEmpty(folderName)) {
            properties.add(AzureBlobStoreSchema.FOLDER_NAME, folderName);
        }
        if (!StringUtils.isNullOrEmpty(keyName)) {
            properties.add("keyName", keyName);
        }
        return createObjectBuilder()
                .add("type", AzureBlobStoreSchema.TYPE)
                .add("properties", properties)
                .build();
    }

    public static final class Builder {

        private String accountName;
        private String containerName;
        private String correlationId;
        private String folderName;
        private String keyName;

        private Builder() {
        }

        public TransferDestination build() {
            Objects.requireNonNull(accountName);
            return new TransferDestination(this);
        }

        public static TransferDestination.Builder newInstance() {
            return new TransferDestination.Builder();
        }

        public Builder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public Builder containerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder folderName(String folderName) {
            this.folderName = folderName;
            return this;
        }

        public Builder keyName(String keyName) {
            this.keyName = keyName;
            return this;
        }
    }
}
