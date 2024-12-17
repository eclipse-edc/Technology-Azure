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

package org.eclipse.edc.test.system.blob;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.azure.blob.BlobStorageConfiguration;
import org.eclipse.edc.azure.blob.api.BlobStoreApiImpl;
import org.eclipse.edc.azure.testfixtures.AzureSettings;
import org.eclipse.edc.azure.testfixtures.TestFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureDataFactoryIntegrationTest;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.vault.azure.AzureVault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.azure.core.util.Configuration.PROPERTY_AZURE_CLIENT_ID;
import static com.azure.core.util.Configuration.PROPERTY_AZURE_CLIENT_SECRET;
import static com.azure.core.util.Configuration.PROPERTY_AZURE_TENANT_ID;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.getenv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.test.system.blob.Constants.POLL_INTERVAL;
import static org.eclipse.edc.test.system.blob.Constants.TIMEOUT;
import static org.eclipse.edc.test.system.blob.ProviderConstants.BLOB_CONTENT;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@AzureDataFactoryIntegrationTest
class AzureDataFactoryTransferIntegrationTest {

    private static final List<Runnable> CONTAINER_CLEANUP = new ArrayList<>();
    private static final String EDC_FS_CONFIG = "edc.fs.config";
    private static final String EDC_VAULT_NAME = "edc.vault.name";
    private static final String PROVIDER_CONTAINER_NAME = UUID.randomUUID().toString();
    private static final AzureSettings AZURE_SETTINGS = new AzureSettings();
    private static final String KEY_VAULT_NAME = AZURE_SETTINGS.getProperty("test.key.vault.name");
    private static final String AZURE_TENANT_ID = getenv(PROPERTY_AZURE_TENANT_ID);
    private static final String AZURE_CLIENT_ID = getenv(PROPERTY_AZURE_CLIENT_ID);
    private static final String AZURE_CLIENT_SECRET = getenv(PROPERTY_AZURE_CLIENT_SECRET);
    private static final long BLOCK_SIZE_IN_MB = 4L;
    private static final int MAX_CONCURRENCY = 2;
    private static final long MAX_SINGLE_UPLOAD_SIZE_IN_MB = 4L;

    @RegisterExtension
    public static final EdcRuntimeExtension CONSUMER = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-storage-transfer-consumer",
            "consumer",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(ConsumerConstants.CONNECTOR_PORT)),
                    Map.entry("web.http.path", ConsumerConstants.CONNECTOR_PATH),
                    Map.entry("web.http.management.port", valueOf(ConsumerConstants.MANAGEMENT_PORT)),
                    Map.entry("web.http.management.path", ConsumerConstants.MANAGEMENT_PATH),
                    Map.entry("web.http.protocol.port", valueOf(ConsumerConstants.PROTOCOL_PORT)),
                    Map.entry("web.http.protocol.path", ConsumerConstants.PROTOCOL_PATH),
                    Map.entry("edc.dsp.callback.address", ConsumerConstants.PROTOCOL_URL),
                    Map.entry(EDC_FS_CONFIG, AzureSettings.azureSettingsFileAbsolutePath()),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(PROPERTY_AZURE_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(PROPERTY_AZURE_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(PROPERTY_AZURE_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );
    @RegisterExtension
    public static final EdcRuntimeExtension PROVIDER = new EdcRuntimeExtension(
            ":system-tests:runtimes:azure-data-factory-transfer-provider",
            "provider",
            Map.ofEntries(
                    Map.entry("web.http.port", valueOf(ProviderConstants.CONNECTOR_PORT)),
                    Map.entry("web.http.path", ProviderConstants.CONNECTOR_PATH),
                    Map.entry("web.http.management.port", valueOf(ProviderConstants.MANAGEMENT_PORT)),
                    Map.entry("web.http.management.path", ProviderConstants.MANAGEMENT_PATH),
                    Map.entry("web.http.protocol.port", valueOf(ProviderConstants.PROTOCOL_PORT)),
                    Map.entry("web.http.protocol.path", ProviderConstants.PROTOCOL_PATH),
                    Map.entry("edc.dsp.callback.address", ProviderConstants.PROTOCOL_URL),
                    Map.entry(EDC_FS_CONFIG, AzureSettings.azureSettingsFileAbsolutePath()),
                    Map.entry(EDC_VAULT_NAME, KEY_VAULT_NAME),
                    Map.entry(PROPERTY_AZURE_CLIENT_ID, AZURE_CLIENT_ID),
                    Map.entry(PROPERTY_AZURE_TENANT_ID, AZURE_TENANT_ID),
                    Map.entry(PROPERTY_AZURE_CLIENT_SECRET, AZURE_CLIENT_SECRET)
            )
    );
    private static final String PROVIDER_STORAGE_ACCOUNT_NAME = AZURE_SETTINGS.getProperty("test.provider.storage.name");
    private static final String CONSUMER_STORAGE_ACCOUNT_NAME = AZURE_SETTINGS.getProperty("test.consumer.storage.name");
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    private final BlobTransferParticipant consumerClient = BlobTransferParticipant.Builder.newInstance()
            .id(ConsumerConstants.PARTICIPANT_ID)
            .name(ConsumerConstants.PARTICIPANT_NAME)
            .managementEndpoint(new Participant.Endpoint(URI.create(ConsumerConstants.MANAGEMENT_URL)))
            .protocolEndpoint(new Participant.Endpoint(URI.create(ConsumerConstants.PROTOCOL_URL)))
            .build();

    private static final BlobStorageConfiguration BLOB_STORE_CORE_EXTENSION_CONFIG =
            new BlobStorageConfiguration(BLOCK_SIZE_IN_MB, MAX_CONCURRENCY, MAX_SINGLE_UPLOAD_SIZE_IN_MB, BLOB_STORE_ENDPOINT_TEMPLATE);

    private final BlobTransferParticipant providerClient = BlobTransferParticipant.Builder.newInstance()
            .id(ProviderConstants.PARTICIPANT_ID)
            .name(ProviderConstants.PARTICIPANT_NAME)
            .managementEndpoint(new Participant.Endpoint(URI.create(ProviderConstants.MANAGEMENT_URL)))
            .protocolEndpoint(new Participant.Endpoint(URI.create(ProviderConstants.PROTOCOL_URL)))
            .build();

    @AfterAll
    static void cleanUp() {
        CONTAINER_CLEANUP.parallelStream().forEach(Runnable::run);
    }

    @Test
    void transferBlob_success() {
        // Arrange
        var secretClient = new SecretClientBuilder()
                .vaultUrl("https://" + KEY_VAULT_NAME + ".vault.azure.net")
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        var vault = new AzureVault(new ConsoleMonitor(), secretClient);
        var consumerAccountKey = Objects.requireNonNull(vault.resolveSecret(format("%s-key1", CONSUMER_STORAGE_ACCOUNT_NAME)));
        var blobStoreApi = new BlobStoreApiImpl(vault, BLOB_STORE_CORE_EXTENSION_CONFIG);

        // Upload a blob with test data on provider blob container
        blobStoreApi.createContainer(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME);
        blobStoreApi.putBlob(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, ProviderConstants.ASSET_FILE, BLOB_CONTENT.getBytes(UTF_8));
        // Add for cleanup
        CONTAINER_CLEANUP.add(() -> blobStoreApi.deleteContainer(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME));

        // Seed data to provider
        var assetId = providerClient.createBlobAsset(PROVIDER_STORAGE_ACCOUNT_NAME, PROVIDER_CONTAINER_NAME, ProviderConstants.ASSET_FILE);
        var policyId = providerClient.createPolicyDefinition(noConstraintPolicy());
        var definitionId = UUID.randomUUID().toString();
        providerClient.createContractDefinition(assetId, definitionId, policyId, policyId);


        var blobServiceClient = TestFunctions.getBlobServiceClient(CONSUMER_STORAGE_ACCOUNT_NAME, consumerAccountKey, TestFunctions.getBlobServiceTestEndpoint(format("https://%s.blob.core.windows.net", CONSUMER_STORAGE_ACCOUNT_NAME)));

        var transferProcessId = consumerClient.requestAssetAndTransferToBlob(providerClient, assetId, CONSUMER_STORAGE_ACCOUNT_NAME);
        await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT).untilAsserted(() -> {
            var state = consumerClient.getTransferProcessState(transferProcessId);
            // should be STARTED or some state after that to make it more robust.
            assertThat(TransferProcessStates.valueOf(state).code()).isGreaterThanOrEqualTo(COMPLETED.code());
        });

        var dataDestination = consumerClient.getDataDestination(transferProcessId);
        assertThat(dataDestination).satisfies(new BlobTransferValidator(blobServiceClient, BLOB_CONTENT, ProviderConstants.ASSET_FILE));
    }
}
