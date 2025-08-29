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
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - added IDS API context
 *
 */

package org.eclipse.edc.test.system.blob;

import com.azure.core.util.BinaryData;
import org.eclipse.edc.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.azure.testfixtures.AzuriteExtension;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.test.system.blob.Constants.POLL_INTERVAL;
import static org.eclipse.edc.test.system.blob.Constants.TIMEOUT;
import static org.eclipse.edc.test.system.blob.ProviderConstants.BLOB_CONTENT;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@AzureStorageIntegrationTest
public class BlobTransferIntegrationTest extends AbstractAzureBlobTest {

    private static final BlobTransferParticipant CONSUMER = BlobTransferParticipant.Builder.newInstance()
            .id(ConsumerConstants.PARTICIPANT_ID)
            .name(ConsumerConstants.PARTICIPANT_NAME)
            .build();

    private static final BlobTransferParticipant PROVIDER = BlobTransferParticipant.Builder.newInstance()
            .id(ProviderConstants.PARTICIPANT_ID)
            .name(ProviderConstants.PARTICIPANT_NAME)
            .build();

    @RegisterExtension
    private static final AzuriteExtension AZURITE = new AzuriteExtension(AZURITE_PORT,
            new AzuriteExtension.Account(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_KEY),
            new AzuriteExtension.Account(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY)
    );

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime(
            "provider",
            ":system-tests:runtimes:azure-storage-transfer-provider"
    ).configurationProvider(() -> PROVIDER.createConfig(AZURITE_PORT)));

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime(
            "consumer",
            ":system-tests:runtimes:azure-storage-transfer-consumer"
    ).configurationProvider(() -> CONSUMER.createConfig(AZURITE_PORT)));

    @ParameterizedTest
    @ArgumentsSource(BlobNamesToTransferProvider.class)
    void transferBlob_success(String assetName, String[] blobsToTransfer) {
        // Upload a blob with test data on provider blob container (in account1).
        createContainer(providerBlobServiceClient, PROVIDER.getContainerName());

        for (var blobToTransfer : blobsToTransfer) {
            providerBlobServiceClient.getBlobContainerClient(PROVIDER.getContainerName())
                    .getBlobClient(blobToTransfer)
                    .upload(BinaryData.fromString(BLOB_CONTENT));
        }

        // Seed data to provider
        var assetId = blobsToTransfer.length > 1 ? PROVIDER.createBlobInFolderAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER.getContainerName(), assetName)
                : PROVIDER.createBlobAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER.getContainerName(), assetName);
        var policyId = PROVIDER.createPolicyDefinition(PolicyFixtures.noConstraintPolicy());
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), policyId, policyId);

        // Write Key to vault
        CONSUMER_RUNTIME.getService(Vault.class).storeSecret(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY);
        PROVIDER_RUNTIME.getService(Vault.class).storeSecret(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_STORAGE_ACCOUNT_KEY);

        var transferProcessId = CONSUMER.requestAssetAndTransferToBlob(PROVIDER, assetId, CONSUMER_STORAGE_ACCOUNT_NAME);
        await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            // should be STARTED or some state after that to make it more robust.
            assertThat(TransferProcessStates.valueOf(state).code()).isGreaterThanOrEqualTo(COMPLETED.code());
        });

        var blobServiceClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, CONSUMER_STORAGE_ACCOUNT_KEY, "http://127.0.0.1:%s/%s".formatted(AZURITE_PORT, CONSUMER_STORAGE_ACCOUNT_NAME));

        var dataDestination = CONSUMER.getDataDestination(transferProcessId);
        for (var blobToTransfer : blobsToTransfer) {
            assertThat(dataDestination).satisfies(new BlobTransferValidator(blobServiceClient, BLOB_CONTENT, blobToTransfer));
        }
    }

    private static class BlobNamesToTransferProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
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
