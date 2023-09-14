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

package org.eclipse.edc.connector.provision.azure.blob;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.api.BlobStoreApiImpl;
import org.eclipse.edc.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@AzureStorageIntegrationTest
class ObjectContainerStatusCheckerIntegrationTest extends AbstractAzureBlobTest {

    private File helloTxt;
    private ObjectContainerStatusChecker checker;

    @BeforeEach
    void setUp() {
        var policy = RetryPolicy.builder().withMaxRetries(1).build();
        helloTxt = TestUtils.getFileFromResourceName("hello.txt");
        Vault vault = mock(Vault.class);

        when(vault.resolveSecret(ACCOUNT_1_NAME + "-key1")).thenReturn(ACCOUNT_1_KEY);
        var blobStoreApi = new BlobStoreApiImpl(vault, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, ACCOUNT_1_NAME));
        checker = new ObjectContainerStatusChecker(blobStoreApi, policy);
    }

    @Test
    void isComplete_noResources() {
        putBlob("hello.txt", helloTxt);
        putBlob(testRunId + ".complete", helloTxt);
        var transferProcess = createTransferProcess(account1ContainerName);

        boolean complete = checker.isComplete(transferProcess, emptyList());

        assertThat(complete).isTrue();
    }

    @Test
    void isComplete_noResources_notComplete() {
        putBlob("hello.txt", helloTxt);

        var tp = createTransferProcess(account1ContainerName);
        assertThat(checker.isComplete(tp, emptyList())).isFalse();
    }

    @Test
    void isComplete_noResources_containerNotExist() {
        var tp = createTransferProcess(account1ContainerName);
        assertThat(checker.isComplete(tp, emptyList())).isFalse();
    }

    @Test
    void isComplete_withResources() {
        putBlob("hello.txt", helloTxt);
        putBlob(testRunId + ".complete", helloTxt);

        var tp = createTransferProcess(account1ContainerName);
        var pr = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, singletonList(pr))).isTrue();
    }

    @Test
    void isComplete_withResources_notComplete() {
        putBlob("hello.txt", helloTxt);

        var tp = createTransferProcess(account1ContainerName);
        var pr = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, singletonList(pr))).isFalse();
    }

    @Test
    void isComplete_withResources_containerNotExist() {
        var tp = createTransferProcess(account1ContainerName);
        var pr = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, singletonList(pr))).isFalse();
    }

    private TransferProcess createTransferProcess(String containerName) {
        return TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType(AzureBlobStoreSchema.TYPE)
                        .dataDestination(DataAddress.Builder.newInstance()
                                .type(AzureBlobStoreSchema.TYPE)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, ACCOUNT_1_NAME)
                                //.property(AzureBlobStoreSchema.BLOB_NAME, ???) omitted on purpose
                                .build())
                        .build())
                .build();
    }

    private ObjectContainerProvisionedResource createProvisionedResource(TransferProcess tp) {
        return ObjectContainerProvisionedResource.Builder.newInstance()
                .containerName(account1ContainerName)
                .accountName(ACCOUNT_1_NAME)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(tp.getId())
                .id(UUID.randomUUID().toString())
                .resourceName(account1ContainerName)
                .build();
    }
}
