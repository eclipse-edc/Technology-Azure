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

package org.eclipse.edc.azure.blob.api;

import com.azure.core.credential.AzureSasCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.AccountSasPermission;
import com.azure.storage.common.sas.AccountSasResourceType;
import com.azure.storage.common.sas.AccountSasService;
import com.azure.storage.common.sas.AccountSasSignatureValues;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.adapter.DefaultBlobAdapter;
import org.eclipse.edc.spi.security.Vault;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BlobStoreApiImpl implements BlobStoreApi {

    private final Vault vault;
    private final String blobstoreEndpointTemplate;
    private final Map<String, BlobServiceClient> cache = new HashMap<>();

    public BlobStoreApiImpl(Vault vault, String blobstoreEndpointTemplate) {
        this.vault = vault;
        this.blobstoreEndpointTemplate = blobstoreEndpointTemplate;
    }

    @Override
    public void createContainer(String accountName, String containerName) {
        getBlobServiceClient(accountName).createBlobContainer(containerName);
    }

    @Override
    public void deleteContainer(String accountName, String containerName) {
        getBlobServiceClient(accountName).deleteBlobContainer(containerName);
    }

    @Override
    public boolean exists(String accountName, String containerName) {
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).exists();
    }

    @Override
    public String createContainerSasToken(String accountName, String containerName, String permissionSpec, OffsetDateTime expiry) {
        var permissions = BlobContainerSasPermission.parse(permissionSpec);
        var values = new BlobServiceSasSignatureValues(expiry, permissions);
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).generateSas(values);
    }

    @Override
    public List<BlobItem> listContainer(String accountName, String containerName) {
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).listBlobs().stream().toList();
    }

    @Override
    public List<BlobItem> listContainerFolder(String accountName, String containerName, String directory) {
        var options = new ListBlobsOptions().setPrefix(directory);
        return getBlobServiceClient(accountName).getBlobContainerClient(containerName).listBlobs(options, null).stream().toList();
    }

    @Override
    public void putBlob(String accountName, String containerName, String blobName, byte[] data) {
        var blobServiceClient = getBlobServiceClient(accountName);
        blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName).upload(BinaryData.fromBytes(data), true);
    }

    @Override
    public String createAccountSas(String accountName, String containerName, String permissionSpec, OffsetDateTime expiry) {
        var permissions = AccountSasPermission.parse(permissionSpec);

        var services = AccountSasService.parse("b");
        var resourceTypes = AccountSasResourceType.parse("co");
        var values = new AccountSasSignatureValues(expiry, permissions, services, resourceTypes);
        return getBlobServiceClient(accountName).generateAccountSas(values);
    }

    @Override
    public byte[] getBlob(String account, String container, String blobName) {
        var client = getBlobServiceClient(account);
        return client.getBlobContainerClient(container).getBlobClient(blobName).downloadContent().toBytes();
    }

    private BlobServiceClient getBlobServiceClient(String accountName) {
        Objects.requireNonNull(accountName, "accountName");

        if (cache.containsKey(accountName)) {
            return cache.get(accountName);
        }

        var accountKey = vault.resolveSecret(accountName + "-key1");
        var endpoint = createEndpoint(accountName);

        var blobServiceClient = accountKey == null ?
                new BlobServiceClientBuilder().credential(new DefaultAzureCredentialBuilder().build())
                        .endpoint(endpoint)
                        .buildClient() :
                new BlobServiceClientBuilder().credential(createCredential(accountKey, accountName))
                        .endpoint(endpoint)
                        .buildClient();

        cache.put(accountName, blobServiceClient);
        return blobServiceClient;
    }

    private StorageSharedKeyCredential createCredential(String accountKey, String accountName) {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    @Override
    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, String sharedKey) {
        var builder = new BlobServiceClientBuilder().credential(new StorageSharedKeyCredential(accountName, sharedKey));
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    @Override
    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, AzureSasCredential credential) {
        var builder = new BlobServiceClientBuilder().credential(credential);
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    @Override
    public BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName) {
        var builder = new BlobServiceClientBuilder().credential(new DefaultAzureCredentialBuilder().build());
        return getBlobAdapter(accountName, containerName, blobName, builder);
    }

    private BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, BlobServiceClientBuilder builder) {
        var blobServiceClient = builder
                .endpoint(createEndpoint(accountName))
                .buildClient();

        var blockBlobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .getBlockBlobClient();

        return new DefaultBlobAdapter(blockBlobClient);
    }

    private String createEndpoint(String accountName) {
        return String.format(blobstoreEndpointTemplate, accountName);
    }
}
