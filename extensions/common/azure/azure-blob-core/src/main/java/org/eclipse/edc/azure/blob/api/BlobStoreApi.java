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
import com.azure.storage.blob.models.BlobItem;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.time.OffsetDateTime;
import java.util.List;

@ExtensionPoint
public interface BlobStoreApi {

    void createContainer(String accountName, String containerName);

    void deleteContainer(String accountName, String containerName);

    boolean exists(String accountName, String containerName);

    String createContainerSasToken(String accountName, String containerName, String accessSpec, OffsetDateTime expiry);

    List<BlobItem> listContainer(String accountName, String containerName);

    void putBlob(String accountName, String containerName, String blobName, byte[] data);

    String createAccountSas(String accountName, String containerName, String racwxdl, OffsetDateTime expiry);

    byte[] getBlob(String account, String container, String blobName);

    BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, String sharedKey);

    BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName, AzureSasCredential credential);

    BlobAdapter getBlobAdapter(String accountName, String containerName, String blobName);
}
