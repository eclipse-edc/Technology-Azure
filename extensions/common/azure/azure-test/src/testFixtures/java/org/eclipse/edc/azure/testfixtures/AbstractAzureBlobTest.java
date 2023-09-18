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

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class AbstractAzureBlobTest {


    protected static final String ACCOUNT_1_NAME = "account1";
    protected static final String ACCOUNT_1_KEY = "key1";
    protected static final String ACCOUNT_2_NAME = "account2";
    protected static final String ACCOUNT_2_KEY = "key2";
    protected static final int AZURITE_PORT = getFreePort();
    @Container
    protected static final GenericContainer<?> AZURITE_CONTAINER = new FixedHostPortGenericContainer<>("mcr.microsoft.com/azure-storage/azurite")
            .withFixedExposedPort(AZURITE_PORT, 10000)
            .withEnv("AZURITE_ACCOUNTS", "%s:%s;%s:%s".formatted(ACCOUNT_1_NAME, ACCOUNT_1_KEY, ACCOUNT_2_NAME, ACCOUNT_2_KEY));
    protected BlobServiceClient blobServiceClient1;
    protected BlobServiceClient blobServiceClient2;
    protected String account1ContainerName;
    protected List<Runnable> containerCleanup = new ArrayList<>();
    protected String testRunId = UUID.randomUUID().toString();

    @BeforeEach
    public void setupClient() {
        account1ContainerName = "storage-container-" + testRunId;

        blobServiceClient1 = TestFunctions.getBlobServiceClient(ACCOUNT_1_NAME, ACCOUNT_1_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, ACCOUNT_1_NAME));
        blobServiceClient2 = TestFunctions.getBlobServiceClient(ACCOUNT_2_NAME, ACCOUNT_2_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, ACCOUNT_2_NAME));

        createContainer(blobServiceClient1, account1ContainerName);
    }

    protected void createContainer(BlobServiceClient client, String containerName) {
        assertFalse(client.getBlobContainerClient(containerName).exists());

        BlobContainerClient blobContainerClient = client.createBlobContainer(containerName);
        assertTrue(blobContainerClient.exists());
        containerCleanup.add(() -> client.deleteBlobContainer(containerName));
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

    protected void putBlob(String name, File file) {
        blobServiceClient1.getBlobContainerClient(account1ContainerName)
                .getBlobClient(name)
                .uploadFromFile(file.getAbsolutePath(), true);
    }
}
