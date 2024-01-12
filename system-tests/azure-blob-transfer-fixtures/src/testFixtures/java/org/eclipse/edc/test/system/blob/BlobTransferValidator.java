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
import org.eclipse.edc.util.collection.CollectionUtil;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BlobTransferValidator implements ThrowingConsumer<Map<String, Object>> {

    private final BlobServiceClient client;
    private final String expectedContent;
    private final String expectedName;
    private final Map<String, String> expectedMetadata;

    public BlobTransferValidator(BlobServiceClient client, String expectedContent, String expectedName, Map<String, String> expectedMetadata) {
        this.client = client;
        this.expectedContent = expectedContent;
        this.expectedName = expectedName;
        this.expectedMetadata = expectedMetadata;
    }

    @Override
    public void acceptThrows(Map<String, Object> destinationProperties) {
        var container = (String) destinationProperties.get("container");
        var destinationBlob = client.getBlobContainerClient(container).getBlobClient(expectedName);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
        var actualBlobContent = destinationBlob.downloadContent().toString();
        assertThat(actualBlobContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(expectedContent);
        if (CollectionUtil.isNotEmpty(expectedMetadata)) {
            var actualMetadata = destinationBlob.getProperties().getMetadata();
            assertThat(actualMetadata)
                    .withFailMessage("Expected metadata not set on transferred file")
                    .containsAllEntriesOf(expectedMetadata);
        }
    }
}
