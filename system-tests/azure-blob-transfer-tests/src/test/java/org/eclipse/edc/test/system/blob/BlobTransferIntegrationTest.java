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
import org.eclipse.edc.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.test.system.blob.Constants.POLL_INTERVAL;
import static org.eclipse.edc.test.system.blob.Constants.TIMEOUT;
import static org.eclipse.edc.test.system.blob.ProviderConstants.BLOB_CONTENT;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@AzureStorageIntegrationTest
public class BlobTransferIntegrationTest extends AbstractAzureBlobTest {
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();

    @RegisterExtension
    protected static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-provider",
            "provider",
            Map.ofEntries(
                    Map.entry("edc.blobstore.endpoint.template", "http://127.0.0.1:" + AZURITE_PORT + "/%s"),
                    Map.entry("edc.test.asset.container.name", PROVIDER_CONTAINER_NAME),
                    Map.entry("web.http.port", valueOf(ProviderConstants.CONNECTOR_PORT)),
                    Map.entry("web.http.path", ProviderConstants.CONNECTOR_PATH),
                    Map.entry("web.http.management.port", valueOf(ProviderConstants.MANAGEMENT_PORT)),
                    Map.entry("web.http.management.path", ProviderConstants.MANAGEMENT_PATH),
                    Map.entry("web.http.protocol.port", valueOf(ProviderConstants.PROTOCOL_PORT)),
                    Map.entry("web.http.protocol.path", ProviderConstants.PROTOCOL_PATH),
                    Map.entry("web.http.control.port", valueOf(ProviderConstants.CONTROL_URL.getPort())),
                    Map.entry("web.http.control.path", ProviderConstants.CONTROL_URL.getPath()),
                    Map.entry(PARTICIPANT_ID, ProviderConstants.PARTICIPANT_ID),
                    Map.entry("edc.dsp.callback.address", ProviderConstants.PROTOCOL_URL),
                    Map.entry("edc.jsonld.http.enabled", Boolean.TRUE.toString())
            )
    );

    @RegisterExtension
    protected static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.ofEntries(
                    Map.entry("edc.blobstore.endpoint.template", "http://127.0.0.1:" + AZURITE_PORT + "/%s"),
                    Map.entry("web.http.port", valueOf(ConsumerConstants.CONNECTOR_PORT)),
                    Map.entry("web.http.path", ConsumerConstants.CONNECTOR_PATH),
                    Map.entry("web.http.management.port", valueOf(ConsumerConstants.MANAGEMENT_PORT)),
                    Map.entry("web.http.management.path", ConsumerConstants.MANAGEMENT_PATH),
                    Map.entry("web.http.protocol.port", valueOf(ConsumerConstants.PROTOCOL_PORT)),
                    Map.entry("web.http.protocol.path", ConsumerConstants.PROTOCOL_PATH),
                    Map.entry(PARTICIPANT_ID, ConsumerConstants.PARTICIPANT_ID),
                    Map.entry("edc.dsp.callback.address", ConsumerConstants.PROTOCOL_URL),
                    Map.entry("edc.jsonld.http.enabled", Boolean.TRUE.toString())
            )
    );

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

    @ParameterizedTest
    @ArgumentsSource(BlobNamesToTransferProvider.class)
    void transferBlob_success(String assetName, String[] blobsToTransfer) {
        // Arrange
        // Upload a blob with test data on provider blob container (in account1).
        createContainer(providerBlobServiceClient, PROVIDER_CONTAINER_NAME);

        for (String blobToTransfer : blobsToTransfer) {
            providerBlobServiceClient.getBlobContainerClient(PROVIDER_CONTAINER_NAME)
                    .getBlobClient(blobToTransfer)
                    .upload(BinaryData.fromString(BLOB_CONTENT));
        }

        // Seed data to provider
        var assetId = blobsToTransfer.length > 1 ? providerClient.createBlobInFolderAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, assetName)
                : providerClient.createBlobAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, assetName);
        var policyId = providerClient.createPolicyDefinition(PolicyFixtures.noConstraintPolicy());
        providerClient.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);

        // Write Key to vault
        consumer.getContext().getService(Vault.class).storeSecret(format("%s-key1", CONSUMER_STORAGE_ACCOUNT_NAME), CONSUMER_STORAGE_ACCOUNT_KEY);
        provider.getContext().getService(Vault.class).storeSecret(format("%s-key1", PROVIDER_STORAGE_ACCOUNT_NAME), PROVIDER_STORAGE_ACCOUNT_KEY);

        var transferProcessId = consumerClient.requestAssetAndTransferToBlob(providerClient, assetId, CONSUMER_STORAGE_ACCOUNT_NAME);
        await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT).untilAsserted(() -> {
            var state = consumerClient.getTransferProcessState(transferProcessId);
            // should be STARTED or some state after that to make it more robust.
            assertThat(TransferProcessStates.valueOf(state).code()).isGreaterThanOrEqualTo(COMPLETED.code());
        });

        var blobServiceClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, CONSUMER_STORAGE_ACCOUNT_NAME));

        var dataDestination = consumerClient.getDataDestination(transferProcessId);
        for (String blobToTransfer : blobsToTransfer) {
            assertThat(dataDestination).satisfies(new BlobTransferValidator(blobServiceClient, BLOB_CONTENT, blobToTransfer));
        }
    }

    private static class BlobNamesToTransferProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of(ProviderConstants.ASSET_PREFIX, new String[]{
                            ProviderConstants.ASSET_PREFIX + 1 + ProviderConstants.ASSET_FILE,
                            ProviderConstants.ASSET_PREFIX + 2 + ProviderConstants.ASSET_FILE,
                            ProviderConstants.ASSET_PREFIX + 3 + ProviderConstants.ASSET_FILE }),
                    Arguments.of(ProviderConstants.ASSET_FILE, new String[]{
                            ProviderConstants.ASSET_FILE }));
        }
    }
}
