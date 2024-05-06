/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.azure.blob.cache;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.azure.blob.utils.BlobStoreUtils.createCredential;
import static org.eclipse.edc.azure.blob.utils.BlobStoreUtils.createEndpoint;

public class AccountCacheImpl implements AccountCache {

    private final Map<String, BlobServiceClient> cache = new HashMap<>();

    public BlobServiceClient getAccount(String accountName) {
        return cache.get(accountName);
    }

    public boolean isAccountInCache(String accountName) {
        Objects.requireNonNull(accountName, "accountName");
        return cache.containsKey(accountName);
    }

    public BlobServiceClient saveAccount(String endpointTemplate, String accountName, String accountKey) {
        var endpoint = createEndpoint(endpointTemplate, accountName);

        var blobServiceClient = accountKey == null ?
                new BlobServiceClientBuilder().credential(new DefaultAzureCredentialBuilder().build())
                        .endpoint(endpoint)
                        .buildClient() :
                new BlobServiceClientBuilder().credential(createCredential(accountKey, accountName))
                        .endpoint(endpoint)
                        .buildClient();

        cache.put(accountName, blobServiceClient);
        return blobServiceClient;
    }
}