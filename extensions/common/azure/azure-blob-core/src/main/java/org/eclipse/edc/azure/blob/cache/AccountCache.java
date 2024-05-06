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

import com.azure.storage.blob.BlobServiceClient;

public interface AccountCache {

    /**
     * Retrieve a cached account.
     *
     * @param accountName The name of the storage account
     * @return The blob service client corresponding to the client to a storage account.
     */
    BlobServiceClient getAccount(String accountName);

    /**
     * Confirms if account is stored in cache.
     *
     * @param accountName The name of the storage account.
     * @return true if account is saved, false otherwise.
     */
    boolean isAccountInCache(String accountName);

    /**
     * Saves the account in cache and retrieve.
     *
     * @param endpointTemplate endpoint base template for the store.
     * @param accountName      The name of the storage account.
     * @param accountKey       The key of the storage account
     * @return The blob service client corresponding to the client to a storage account.
     */
    BlobServiceClient saveAccount(String endpointTemplate, String accountName, String accountKey);
}
