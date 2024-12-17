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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.azure.blob.api;

import org.eclipse.edc.azure.blob.BlobStorageConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createSharedKey;

class BlobStoreApiImplTest {

    @Test
    void getBlobAdapter_succeeds() {
        var blobStoreCoreExtensionConfig =
                new BlobStorageConfiguration(4L, 2, 4L, "https://%s.blob.core.windows.net");
        var service = new BlobStoreApiImpl(null, blobStoreCoreExtensionConfig);
        assertThatNoException()
                .isThrownBy(() -> service.getBlobAdapter(
                        createAccountName(),
                        createContainerName(),
                        createBlobName(),
                        createSharedKey()));
    }
}
