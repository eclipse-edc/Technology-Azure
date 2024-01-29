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
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.edc.test.system.blob;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.test.system.utils.Participant;
import org.eclipse.edc.test.system.utils.PolicyFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CORRELATION_ID;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.test.system.blob.Constants.POLL_INTERVAL;
import static org.eclipse.edc.test.system.blob.Constants.TIMEOUT;
import static org.eclipse.edc.test.system.blob.ConsumerConstants.CONSUMER_PROPERTIES;
import static org.eclipse.edc.test.system.blob.ProviderConstants.ASSET_FILE;
import static org.eclipse.edc.test.system.blob.ProviderConstants.BLOB_CONTENT;
import static org.eclipse.edc.test.system.blob.ProviderConstants.PROVIDER_PROPERTIES;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * This test class is used to test the blob transfer between two blob accounts.
 * In contrast to {@link BlobTransferIntegrationTest}, this test class uses a pre-provisioned container on the consumer side.
 * The pre-provisioned container is created before the transfer process is started.
 * For this test to work, there must not be a provisioning extension configured on the consumer side, otherwise the
 * data destination properties will be replaced as a whole, losing the provided properties, e.g. the correlation id.
 * Please refer to the moduleName argument of the {@link PreProvisionedTransferIntegrationTest#consumer} configuration.
 **/
@Testcontainers
@AzureStorageIntegrationTest
public class PreProvisionedTransferIntegrationTest extends AbstractAzureBlobTest {
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final String CONSUMER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final String CONSUMER_KEY_NAME = format("%s-sas", CONSUMER_STORAGE_ACCOUNT_NAME);
    private static final String BLOB_CORRELATION_ID = UUID.randomUUID().toString();
    private static final String BLOB_FOLDER_NAME = UUID.randomUUID().toString();

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-provider",
            "provider",
            new HashMap<>() {
                {
                    putAll(PROVIDER_PROPERTIES);
                    put("edc.blobstore.endpoint.template", "http://127.0.0.1:" + AZURITE_PORT + "/%s");
                }
            });

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer-non-provisioning",
            "consumer",
            new HashMap<>() {
                {
                    putAll(CONSUMER_PROPERTIES);
                    put("edc.blobstore.endpoint.template", "http://127.0.0.1:" + AZURITE_PORT + "/%s");
                }
            });

    private final BlobTransferParticipant consumerClient = BlobTransferParticipant.Builder.newInstance()
            .id(ConsumerConstants.PARTICIPANT_ID)
            .name(ConsumerConstants.PARTICIPANT_NAME)
            .managementEndpoint(new Participant.Endpoint(URI.create(ConsumerConstants.MANAGEMENT_URL)))
            .protocolEndpoint(new Participant.Endpoint(URI.create(ConsumerConstants.PROTOCOL_URL)))
            .build();

    private final BlobTransferParticipant providerClient = BlobTransferParticipant.Builder.newInstance()
            .id(ProviderConstants.PARTICIPANT_ID)
            .name(ProviderConstants.PARTICIPANT_NAME)
            .managementEndpoint(new Participant.Endpoint(URI.create(ProviderConstants.MANAGEMENT_URL)))
            .protocolEndpoint(new Participant.Endpoint(URI.create(ProviderConstants.PROTOCOL_URL)))
            .build();

    @Test
    void transferBlob_success() throws JsonProcessingException {
        // Arrange
        // Upload a blob with test data on provider blob container (in account1).
        createContainer(providerBlobServiceClient, PROVIDER_CONTAINER_NAME);
        providerBlobServiceClient.getBlobContainerClient(PROVIDER_CONTAINER_NAME)
                .getBlobClient(ProviderConstants.ASSET_FILE)
                .upload(BinaryData.fromString(BLOB_CONTENT));

        // Seed data to provider
        var assetId = providerClient.createBlobAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, ProviderConstants.ASSET_FILE);
        var policyId = providerClient.createPolicyDefinition(PolicyFixtures.noConstraintPolicy());
        providerClient.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);

        // Pre-provision container on consumer side
        createContainer(consumerBlobServiceClient, CONSUMER_CONTAINER_NAME);

        // Write keys to vault
        consumer.getContext().getService(Vault.class).storeSecret(format("%s-key1", CONSUMER_STORAGE_ACCOUNT_NAME), CONSUMER_STORAGE_ACCOUNT_KEY);
        provider.getContext().getService(Vault.class).storeSecret(format("%s-key1", PROVIDER_STORAGE_ACCOUNT_NAME), PROVIDER_STORAGE_ACCOUNT_KEY);

        // Write SAS write-only token for consumer account (account2) to provider vault
        var token = new ObjectMapper().writeValueAsString(createWriteOnlyToken(consumerBlobServiceClient, OffsetDateTime.now().plusHours(1)));
        provider.getContext().getService(Vault.class).storeSecret(CONSUMER_KEY_NAME, token);

        var transferProcessId = consumerClient.requestAssetAndTransferToBlob(providerClient, assetId,
                TransferDestination.Builder.newInstance()
                        .accountName(CONSUMER_STORAGE_ACCOUNT_NAME)
                        .keyName(CONSUMER_KEY_NAME)
                        .correlationId(BLOB_CORRELATION_ID)
                        .folderName(BLOB_FOLDER_NAME)
                        .containerName(CONSUMER_CONTAINER_NAME));

        await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT).untilAsserted(() -> {
            var state = consumerClient.getTransferProcessState(transferProcessId);
            // should be STARTED or some state after that to make it more robust.
            assertThat(TransferProcessStates.valueOf(state).code()).isGreaterThanOrEqualTo(COMPLETED.code());
        });

        var blobServiceClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, CONSUMER_STORAGE_ACCOUNT_NAME));

        var dataDestination = consumerClient.getDataDestination(transferProcessId);
        assertThat(dataDestination).satisfies(new BlobTransferValidator(blobServiceClient, BLOB_CONTENT, BLOB_FOLDER_NAME + "/" + ASSET_FILE, Map.ofEntries(Map.entry(CORRELATION_ID, BLOB_CORRELATION_ID))));
    }

}
