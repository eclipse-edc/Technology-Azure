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

package org.eclipse.edc.vault.azure;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.util.polling.SyncPoller;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

/**
 * Implements a vault backed by Azure Vault.
 */
public class AzureVault implements Vault {

    private static final String ALLOWED_CHARACTERS_REGEX = "^[a-zA-Z0-9-]*$";
    private static final String DISALLOWED_CHARACTERS_REGEX = "[^a-zA-Z0-9-]+";
    private static final String STARTS_WITH_LETTER_REGEX = "^[A-Za-z].*$";
    private static final String LETTER_PREFIX = "x-";
    private final SecretClient secretClient;
    private final Monitor monitor;

    public AzureVault(Monitor monitor, SecretClient secretClient) {
        this.monitor = monitor;
        this.secretClient = secretClient;
    }

    @Override
    public @Nullable String resolveSecret(String key) {
        var sanitizedKey = sanitizeKey(key);
        try {
            var secret = secretClient.getSecret(sanitizedKey);
            return secret.getValue();
        } catch (ResourceNotFoundException ex) {
            monitor.debug(format("Secret %s not found", sanitizedKey));
            return null;
        } catch (Exception ex) {
            monitor.severe("Error accessing secret " + key, ex);
            return null;
        }
    }

    @Override
    public Result<Void> storeSecret(String key, String value) {
        try {
            var sanitizedKey = sanitizeKey(key);
            secretClient.setSecret(sanitizedKey, value);
            monitor.debug("storing secret successful");
            return Result.success();
        } catch (Exception ex) {
            monitor.severe("Error storing secret", ex);
            return Result.failure(ex.getMessage());
        }
    }

    @Override
    public Result<Void> deleteSecret(String key) {
        var sanitizedKey = sanitizeKey(key);
        SyncPoller<DeletedSecret, Void> poller = null;
        try {
            poller = secretClient.beginDeleteSecret(sanitizedKey);
            monitor.debug("Begin deleting secret");
            poller.waitForCompletion(Duration.ofMinutes(1));

            monitor.debug("deletion complete");
            return Result.success();
        } catch (ResourceNotFoundException ex) {
            monitor.severe("Error deleting secret - does not exist!");
            return Result.failure(ex.getMessage());
        } catch (RuntimeException re) {
            monitor.severe("Error deleting secret", re);

            if (re.getCause() != null && re.getCause() instanceof TimeoutException) {
                try {
                    if (poller != null) {
                        poller.cancelOperation();
                    }
                } catch (Exception e) {
                    monitor.severe("Failed to abort the deletion. ", e);
                    return Result.failure(e.getMessage());
                }
            }
            return Result.failure(re.getMessage());
        } finally {
            try {
                secretClient.purgeDeletedSecret(sanitizedKey);
            } catch (Exception e) {
                monitor.severe("Error purging secret from AzureVault", e);
            }
        }
    }

    @NotNull
    private String sanitizeKey(String key) {
        if (!key.matches(STARTS_WITH_LETTER_REGEX)) {
            monitor.debug("AzureVault: key does not start with a letter. Prefixing with " + LETTER_PREFIX);
            key = LETTER_PREFIX + key;
        }
        if (!key.matches(ALLOWED_CHARACTERS_REGEX)) {
            monitor.debug("AzureVault: key contained a disallowed character. Only [a-zA-Z0-9-] are allowed. replaced with '-'");
            key = key.replaceAll(DISALLOWED_CHARACTERS_REGEX, "-");
        }
        //should we truncate the size to 127 characters or let it blow up?
        return key;
    }
}
