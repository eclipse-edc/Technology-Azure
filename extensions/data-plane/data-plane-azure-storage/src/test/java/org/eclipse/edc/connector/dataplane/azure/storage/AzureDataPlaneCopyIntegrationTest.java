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

package org.eclipse.edc.connector.dataplane.azure.storage;

import com.azure.core.credential.AzureSasCredential;
import com.azure.core.util.BinaryData;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.BlobStorageConfiguration;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.adapter.DefaultBlobAdapter;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.azure.blob.api.BlobStoreApiImpl;
import org.eclipse.edc.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.azure.testfixtures.AzuriteExtension;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProviderImpl;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.CommonBlobMetadataDecorator;
import org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSinkFactory;
import org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSourceFactory;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.TYPE;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled
@Testcontainers
@AzureStorageIntegrationTest
class AzureDataPlaneCopyIntegrationTest extends AbstractAzureBlobTest {

    private final TypeManager typeManager = new JacksonTypeManager();

    private final RetryPolicy<Object> policy = RetryPolicy.builder().withMaxRetries(1).build();
    private final String sinkContainerName = createContainerName();
    private final String blobName = createBlobName();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Monitor monitor = mock();
    private final Vault vault = mock();
    private final long blockSizeInMb = 4L;
    private final int maxConcurrency = 2;
    private final long maxSingleUploadSizeInMb = 4L;
    private final BlobStorageConfiguration blobStoreCoreExtensionProviderConfig =
            new BlobStorageConfiguration(blockSizeInMb, maxConcurrency, maxSingleUploadSizeInMb,
                    "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, PROVIDER_STORAGE_ACCOUNT_NAME));
    private final BlobStorageConfiguration blobStoreCoreExtensionConsumerConfig =
            new BlobStorageConfiguration(blockSizeInMb, maxConcurrency, maxSingleUploadSizeInMb,
                    "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, CONSUMER_STORAGE_ACCOUNT_NAME));

    private final BlobStoreApi account1Api = new BlobStoreApiImpl(vault, blobStoreCoreExtensionProviderConfig);
    private final BlobStoreApi account2Api = new BlobStoreApiImpl(vault, blobStoreCoreExtensionConsumerConfig);

    @RegisterExtension
    private static AzuriteExtension azurite = new AzuriteExtension(AZURITE_PORT,
            new AzuriteExtension.Account(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_KEY),
            new AzuriteExtension.Account(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY)
    );

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("participant-id")
            .identity("identity")
            .build();
    private final SingleParticipantContextSupplier participantContextSupplier = ()  -> ServiceResult.success(participantContext);

    @BeforeEach
    void setUp() {
        createContainer(consumerBlobServiceClient, sinkContainerName);
    }

    @Test
    void transfer_success() {
        var content = "test-content";
        providerBlobServiceClient.getBlobContainerClient(providerContainerName)
                .getBlobClient(blobName)
                .upload(BinaryData.fromString(content));

        var account1KeyName = "test-account-key-name1";
        var source = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_NAME)
                .property(CONTAINER_NAME, providerContainerName)
                .property(BLOB_NAME, blobName)
                .keyName(account1KeyName)
                .build();
        when(vault.resolveSecret(account1KeyName)).thenReturn(PROVIDER_STORAGE_ACCOUNT_KEY);

        var account2KeyName = "test-account-key-name2";
        var destination = DataAddress.Builder.newInstance()
                .type(TYPE)
                .property(ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_NAME)
                .property(CONTAINER_NAME, sinkContainerName)
                .keyName(account2KeyName)
                .build();

        when(vault.resolveSecret(CONSUMER_STORAGE_ACCOUNT_NAME))
                .thenReturn(CONSUMER_STORAGE_ACCOUNT_KEY);

        var account2SasToken = account2Api.createContainerSasToken(CONSUMER_STORAGE_ACCOUNT_NAME, sinkContainerName,
                "w", OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(1).truncatedTo(ChronoUnit.DAYS));

        var secretToken = new AzureSasToken(account2SasToken, Long.MAX_VALUE);
        when(vault.resolveSecret(account2KeyName))
                .thenReturn(typeManager.writeValueAsString(secretToken));

        var request = DataFlowStartMessage.Builder.newInstance()
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .build();

        var dataSource = new AzureStorageDataSourceFactory(account1Api, policy, monitor, vault)
                .createSource(request);

        var partitionSize = 5;

        var account2ApiPatched = new BlobStoreApiImpl(vault, blobStoreCoreExtensionConsumerConfig) {
            @Override
            public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, AzureSasCredential credential) {
                var blobClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY)
                        .getBlobContainerClient(containerName).getBlobClient(blobName).getBlockBlobClient();
                return new DefaultBlobAdapter(blobClient, blobStoreCoreExtensionProviderConfig);
            }
        };

        var metadataProvider = new BlobMetadataProviderImpl(monitor);
        metadataProvider.registerDecorator(new CommonBlobMetadataDecorator("participant-id", "connector-id"));
        var dataSinkFactory = new AzureStorageDataSinkFactory(participantContextSupplier, account2ApiPatched, executor, partitionSize, monitor, vault, typeManager, metadataProvider);
        var dataSink = dataSinkFactory.createSink(request);

        assertThat(dataSink.transfer(dataSource))
                .succeedsWithin(500, TimeUnit.MILLISECONDS)
                .satisfies(transferResult -> assertThat(transferResult.succeeded()).isTrue());

        var destinationBlob = consumerBlobServiceClient
                .getBlobContainerClient(sinkContainerName)
                .getBlobClient(blobName);
        assertThat(destinationBlob.exists())
                .withFailMessage("should have copied blob between containers")
                .isTrue();
        assertThat(destinationBlob.downloadContent())
                .asString()
                .isEqualTo(content);

        var metadata = destinationBlob.getBlockBlobClient().getProperties().getMetadata();
        assertThat(metadata.get("originalName")).isEqualTo(blobName);

        assertThat(metadata.get("requestId")).isEqualTo(request.getId());
        assertThat(metadata.get("processId")).isEqualTo(request.getProcessId());

        assertThat(metadata.get("connectorId")).isEqualTo("connector-id");
        assertThat(metadata.get("participantId")).isEqualTo("participant-id");
    }
}
