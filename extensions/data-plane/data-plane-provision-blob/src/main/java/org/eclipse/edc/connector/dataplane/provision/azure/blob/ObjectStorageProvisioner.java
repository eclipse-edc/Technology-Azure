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

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.provision.azure.AzureProvisionConfiguration;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static dev.failsafe.Failsafe.with;

public class ObjectStorageProvisioner implements Provisioner {
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final BlobStoreApi blobStoreApi;
    private final AzureProvisionConfiguration azureProvisionConfiguration;
    private final Vault vault;
    private final TypeManager typeManager;
    private final SingleParticipantContextSupplier participantContextSupplier;

    public ObjectStorageProvisioner(RetryPolicy<Object> retryPolicy, Monitor monitor, BlobStoreApi blobStoreApi,
                                    AzureProvisionConfiguration azureProvisionConfiguration,
                                    Vault vault,
                                    TypeManager typeManager, SingleParticipantContextSupplier participantContextSupplier) {
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.blobStoreApi = blobStoreApi;
        this.azureProvisionConfiguration = azureProvisionConfiguration;
        this.vault = vault;
        this.typeManager = typeManager;
        this.participantContextSupplier = participantContextSupplier;
    }

    @Override
    public String supportedType() {
        return AzureBlobStoreSchema.TYPE;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResource provisionResource) {
        var dataAddress = provisionResource.getDataAddress();
        String containerName = dataAddress.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME);
        String accountName = dataAddress.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME);

        monitor.debug("Azure Storage Container request submitted: " + containerName);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(this.azureProvisionConfiguration.tokenExpiryTime());

        return checkContainerExists(accountName, containerName)
                .thenCompose(exists -> {
                    if (exists) {
                        return reusingExistingContainer(containerName);
                    } else {
                        return createContainer(containerName, accountName);
                    }
                })
                .thenCompose(empty -> createContainerSasToken(containerName, accountName, expiryTime))
                .thenApply(writeOnlySas -> {
                    // Ensure resource name is unique to avoid key collisions in local and remote vaults
                    String resourceName = provisionResource.getFlowId() + "-container-secret";

                    var secretToken = new AzureSasToken("?" + writeOnlySas, expiryTime.toInstant().toEpochMilli());

                    var participantContext = participantContextSupplier.get();

                    if (participantContext.succeeded()) {
                        try {
                            vault.storeSecret(participantContext.getContent().getParticipantContextId(), resourceName, typeManager.getMapper().writeValueAsString(secretToken));
                        } catch (JsonProcessingException e) {
                            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Cannot serialize secret token: " + e.getMessage());
                        }

                        var response = ProvisionedResource.Builder
                                .from(provisionResource)
                                .dataAddress(DataAddress.Builder.newInstance()
                                        .properties(provisionResource.getDataAddress().getProperties())
                                        .keyName(resourceName)
                                        .build())
                                .build();
                        return StatusResult.success(response);
                    } else {
                        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Cannot access participant context: " + participantContext.getFailure().getMessages());
                    }
                });
    }

    @NotNull
    private CompletableFuture<Boolean> checkContainerExists(String accountName, String containerName) {
        return with(retryPolicy).getAsync(() -> blobStoreApi.exists(accountName, containerName));
    }

    @NotNull
    private CompletableFuture<Void> reusingExistingContainer(String containerName) {
        monitor.debug("ObjectStorageProvisioner: re-use existing container " + containerName);
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    private CompletableFuture<Void> createContainer(String containerName, String accountName) {
        return with(retryPolicy)
                .runAsync(() -> {
                    blobStoreApi.createContainer(accountName, containerName);
                    monitor.debug("ObjectStorageProvisioner: created a new container " + containerName);
                });
    }

    @NotNull
    private CompletableFuture<String> createContainerSasToken(String containerName, String accountName, OffsetDateTime expiryTime) {
        return with(retryPolicy)
                .getAsync(() -> {
                    monitor.debug("ObjectStorageProvisioner: obtained temporary SAS token (write-only)");
                    return blobStoreApi.createContainerSasToken(accountName, containerName, "w", expiryTime);
                });
    }
}
