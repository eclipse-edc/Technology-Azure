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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createSharedKey;

class BlobStoreApiImplTest {

    @Test
    void getBlobAdapter_succeeds() {
        var blockSizeInMb = 4L;
        var maxConcurrency = 2;
        var maxSingleUploadSizeInMb = 4L;
        var service = new BlobStoreApiImpl(null, "https://%s.blob.core.windows.net",
                blockSizeInMb, maxConcurrency, maxSingleUploadSizeInMb);
        assertThatNoException()
                .isThrownBy(() -> service.getBlobAdapter(
                        createAccountName(),
                        createContainerName(),
                        createBlobName(),
                        createSharedKey()));
    }
}
