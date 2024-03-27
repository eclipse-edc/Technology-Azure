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

package org.eclipse.edc.azure.testfixtures;

import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractAzureBlobTest {


    protected static final String PROVIDER_STORAGE_ACCOUNT_NAME = "account1";
    protected static final String PROVIDER_STORAGE_ACCOUNT_KEY = "key1";
    protected static final String CONSUMER_STORAGE_ACCOUNT_NAME = "account2";
    protected static final String CONSUMER_STORAGE_ACCOUNT_KEY = "key2";
    protected static final int AZURITE_PORT = getFreePort();
    @Container
    protected static final GenericContainer<?> AZURITE_CONTAINER = new FixedHostPortGenericContainer<>("mcr.microsoft.com/azure-storage/azurite")
            .withFixedExposedPort(AZURITE_PORT, 10000)
            .withEnv("AZURITE_ACCOUNTS", "%s:%s;%s:%s".formatted(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_KEY, CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY));
    protected BlobServiceClient providerBlobServiceClient;
    protected BlobServiceClient consumerBlobServiceClient;
    protected String providerContainerName;
    protected List<Runnable> containerCleanup = new ArrayList<>();
    protected String testRunId = UUID.randomUUID().toString();

    @BeforeEach
    public void setupClient() {
        providerContainerName = "storage-container-" + testRunId;

        providerBlobServiceClient = TestFunctions.getBlobServiceClient(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, PROVIDER_STORAGE_ACCOUNT_NAME));
        consumerBlobServiceClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, CONSUMER_STORAGE_ACCOUNT_NAME));

        createContainer(providerBlobServiceClient, providerContainerName);
    }

    @AfterEach
    public void teardown() {
        for (var cleanup : containerCleanup) {
            try {
                cleanup.run();
            } catch (Exception ex) {
                fail("teardown failed, subsequent tests might fail as well!");
            }
        }
    }

    protected void createContainer(BlobServiceClient client, String containerName) {
        assertFalse(client.getBlobContainerClient(containerName).exists());

        var blobContainerClient = client.createBlobContainer(containerName);
        assertTrue(blobContainerClient.exists());
        containerCleanup.add(() -> client.deleteBlobContainer(containerName));
    }
}
