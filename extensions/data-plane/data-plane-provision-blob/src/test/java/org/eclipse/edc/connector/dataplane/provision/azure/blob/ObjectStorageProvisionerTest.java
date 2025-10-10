/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.dataplane.provision.azure.blob;

import com.azure.storage.blob.models.BlobStorageException;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.provision.azure.AzureProvisionConfiguration;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.FOLDER_NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStorageProvisionerTest {

    private final BlobStoreApi blobStoreApiMock = mock(BlobStoreApi.class);
    private final AzureProvisionConfiguration azureProvisionConfiguration = mock(AzureProvisionConfiguration.class);
    private final Vault vault = mock(Vault.class);
    private final TypeManager typeManager = new JacksonTypeManager();
    private ObjectStorageProvisioner provisioner;

    @BeforeEach
    void setup() {
        RetryPolicy<Object> retryPolicy = RetryPolicy.builder().withMaxRetries(0).build();
        provisioner = new ObjectStorageProvisioner(retryPolicy, mock(Monitor.class), blobStoreApiMock, azureProvisionConfiguration, vault, typeManager);
    }

    @Test
    void provision_withFolder_andBlob_success() {
        var accountName = "test-account";
        var containerName = "test-container";
        Map<String, Object> destinationProps = Map.of(
                EDC_NAMESPACE + ACCOUNT_NAME, "test-account",
                EDC_NAMESPACE + CONTAINER_NAME, "test-container",
                EDC_NAMESPACE + FOLDER_NAME, "test-folder",
                EDC_NAMESPACE + BLOB_NAME, "test-blob"
        );
        var toProvision = createProvisionedResource(destinationProps).flowId("some-flow-id").build();

        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var provisioned = provisioner.provision(toProvision).join().getContent();

        assertThat(provisioned).isInstanceOfSatisfying(ProvisionedResource.class, resource -> {
            assertThat(resource.getFlowId()).isEqualTo("some-flow-id");
            assertThat(resource.getDataAddress().getStringProperty(EDC_NAMESPACE + FOLDER_NAME)).isEqualTo("test-folder");
            assertThat(resource.getDataAddress().getStringProperty(EDC_NAMESPACE + BLOB_NAME)).isEqualTo("test-blob");
        });

        verify(blobStoreApiMock).exists(anyString(), anyString());
        verify(blobStoreApiMock).createContainer(accountName, containerName);
        verify(vault).storeSecret(anyString(), anyString());
    }

    @Test
    void provision_success() {
        var accountName = "test-account";
        var containerName = "test-container";
        Map<String, Object> destinationProps = Map.of(
                ACCOUNT_NAME, accountName,
                CONTAINER_NAME, containerName
        );
        var toProvision = createProvisionedResource(destinationProps).flowId("some-flow-id").build();
        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var provisioned = provisioner.provision(toProvision).join().getContent();

        assertThat(provisioned).isInstanceOfSatisfying(ProvisionedResource.class, resource -> {
            assertThat(resource.getFlowId()).isEqualTo("some-flow-id");
            assertThat(resource.getDataAddress().getStringProperty(EDC_NAMESPACE + FOLDER_NAME)).isNull();
        });

        verify(blobStoreApiMock).exists(anyString(), anyString());
        verify(blobStoreApiMock).createContainer(accountName, containerName);
        verify(vault).storeSecret(anyString(), anyString());
    }

    @Test
    void provision_unique_secret() {
        var accountName = "test-account";
        var containerName = "test-container";
        Map<String, Object> destinationProps = Map.of(
                ACCOUNT_NAME, accountName,
                CONTAINER_NAME, containerName
        );
        when(blobStoreApiMock.exists(accountName, containerName)).thenReturn(true);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var toProvision = createProvisionedResource(destinationProps).flowId("some-flow-id").build();
        var provisioned = provisioner.provision(toProvision).join().getContent();

        var toProvision2 = createProvisionedResource(destinationProps).flowId("some-flow-id-2").build();
        var provisioned2 = provisioner.provision(toProvision2).join().getContent();
        assertThat(provisioned.getDataAddress().getKeyName()).isNotEqualTo(provisioned2.getDataAddress().getKeyName());
    }

    @Test
    void provision_container_already_exists() {
        var accountName = "test-account";
        var containerName = "test-container";
        Map<String, Object> destinationProps = Map.of(
                ACCOUNT_NAME, accountName,
                CONTAINER_NAME, containerName
        );
        var toProvision = createProvisionedResource(destinationProps).flowId("some-flow-id").build();
        when(blobStoreApiMock.exists(accountName, containerName)).thenReturn(true);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        provisioner.provision(toProvision);

        verify(blobStoreApiMock, never()).createContainer(anyString(), anyString());
        verify(blobStoreApiMock).createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any());
    }

    @Test
    void provision_no_key_found_in_vault() {
        var accountName = "test-account";
        var containerName = "test-container";
        Map<String, Object> destinationProps = Map.of(
                ACCOUNT_NAME, accountName,
                CONTAINER_NAME, containerName
        );
        var toProvision = createProvisionedResource(destinationProps).flowId("some-flow-id").build();
        when(blobStoreApiMock.exists(any(), anyString()))
                .thenThrow(new IllegalArgumentException("No Object Storage credential found in vault"));

        assertThatThrownBy(() -> provisioner.provision(toProvision).join()).hasCauseInstanceOf(IllegalArgumentException.class);
        verify(blobStoreApiMock).exists(any(), any());
    }

    @Test
    void provision_key_not_authorized() {
        var accountName = "test-account";
        var containerName = "test-container";
        Map<String, Object> destinationProps = Map.of(
                ACCOUNT_NAME, accountName,
                CONTAINER_NAME, containerName
        );
        var toProvision = createProvisionedResource(destinationProps).flowId("some-flow-id").build();
        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        doThrow(new BlobStorageException("not authorized", null, null))
                .when(blobStoreApiMock).createContainer(accountName, containerName);

        assertThatThrownBy(() -> provisioner.provision(toProvision).join()).hasCauseInstanceOf(BlobStorageException.class);
        verify(blobStoreApiMock).exists(anyString(), anyString());
    }

    private ProvisionResource.Builder createProvisionedResource(Map<String, Object> destinationProperties) {
        return ProvisionResource.Builder.newInstance()
                .dataAddress(DataAddress.Builder.newInstance()
                        .type(AzureBlobStoreSchema.TYPE)
                        .properties(destinationProperties)
                        .build())
                .type(AzureBlobStoreSchema.TYPE);
    }

}
