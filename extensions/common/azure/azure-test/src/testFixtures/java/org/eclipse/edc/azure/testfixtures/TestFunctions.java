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

package org.eclipse.edc.azure.testfixtures;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class TestFunctions {
    private TestFunctions() {
    }

    @NotNull
    public static BlobServiceClient getBlobServiceClient(String accountName, String key) {
        var connectionString = new LocalConnectionStringBuilder()
                .http().account(accountName).key(key).endpoints(accountName).build();
        var client = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        client.getAccountInfo();
        return client;
    }

    @NotNull
    public static BlobServiceClient getBlobServiceClient(String accountName, String key, String endpoint) {
        var client = new BlobServiceClientBuilder()
                .credential(new StorageSharedKeyCredential(accountName, key))
                .endpoint(endpoint)
                .buildClient();

        client.getAccountInfo();
        return client;
    }

    @NotNull
    public static BlockBlobClient getBlobClient(String accountName, String containerName, String blobName, String token) {
        var connectionString = new LocalConnectionStringBuilder()
                .http().account(accountName).sharedAccessSignature(token).endpoints(accountName).build();
        var serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        return serviceClient.getBlobContainerClient(containerName).getBlobClient(blobName).getBlockBlobClient();
    }

    public static String getBlobServiceTestEndpoint(String accountName) {
        return "http://127.0.0.1:10000/" + accountName;
    }

    private static class LocalConnectionStringBuilder {

        private final List<String> elements = new ArrayList<>();

        public String build() {
            return String.join(";", elements) + ";";
        }

        public LocalConnectionStringBuilder endpoints(String accountName) {
            elements.add("BlobEndpoint=http://127.0.0.1:10000/" + accountName);

            // Even though not used, a malformed connection string exception is thrown if not present
            elements.add("QueueEndpoint=http://127.0.0.1:10001/" + accountName);
            elements.add("TableEndpoint=http://127.0.0.1:10002/" + accountName);
            elements.add("FileEndpoint=http://127.0.0.1:10003/" + accountName);
            return this;
        }

        public LocalConnectionStringBuilder http() {
            elements.add("DefaultEndpointsProtocol=http");
            return this;
        }

        public LocalConnectionStringBuilder account(String accountName) {
            elements.add("AccountName=" + accountName);
            return this;
        }

        public LocalConnectionStringBuilder key(String accountKey) {
            elements.add("AccountKey=" + accountKey);
            return this;
        }

        public LocalConnectionStringBuilder sharedAccessSignature(String sharedAccessSignature) {
            elements.add("SharedAccessSignature=" + sharedAccessSignature);
            return this;
        }
    }
}
