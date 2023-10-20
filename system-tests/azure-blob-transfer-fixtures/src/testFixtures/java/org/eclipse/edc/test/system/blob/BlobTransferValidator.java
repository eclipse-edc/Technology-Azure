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

import com.azure.storage.blob.BlobServiceClient;
import org.assertj.core.api.ThrowingConsumer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BlobTransferValidator implements ThrowingConsumer<Map<String, Object>> {

    private final BlobServiceClient client;
    private final String expectedContent;

    public BlobTransferValidator(BlobServiceClient client, String expectedContent) {
        this.client = client;
        this.expectedContent = expectedContent;
    }
    
    @Override
    public void acceptThrows(Map<String, Object> destinationProperties) {
        var container = (String) destinationProperties.get("container");
        var destinationBlob = client.getBlobContainerClient(container).getBlobClient(ProviderConstants.ASSET_FILE);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(expectedContent);
    }
}
