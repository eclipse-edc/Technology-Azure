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
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.azure.blob.utils.BlobStoreUtils.createEndpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountCacheImplTest {

    private static final String ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private static final String ACCOUNT_NAME = "test_account";
    private static final String ACCOUNT_KEY = "test_key";

    private final AccountCache accountCache = new AccountCacheImpl();

    @Test
    void isAccountInCache_succeeds() {
        var resultNotInCache = accountCache.isAccountInCache(ACCOUNT_NAME);
        assertFalse(resultNotInCache);
        addAccount();
        var resultInCache = accountCache.isAccountInCache(ACCOUNT_NAME);
        assertTrue(resultInCache);
    }

    @Test
    void saveAccount_succeeds() {
        var result = addAccount();
        assertEquals(result.getAccountName(), ACCOUNT_NAME);
        assertEquals(result.getAccountUrl(), createEndpoint(ENDPOINT_TEMPLATE, ACCOUNT_NAME));
    }

    @Test
    void getAccount_succeeds() {
        addAccount();
        var result = accountCache.getAccount(ACCOUNT_NAME);
        assertEquals(result.getAccountName(), ACCOUNT_NAME);
    }

    private BlobServiceClient addAccount() {
        return accountCache.saveAccount(ENDPOINT_TEMPLATE, ACCOUNT_NAME, ACCOUNT_KEY);
    }
}