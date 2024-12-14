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

package org.eclipse.edc.azure.blob.adapter;

import com.azure.core.util.ProgressListener;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.options.BlockBlobOutputStreamOptions;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.implementation.Constants;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Implementation of {@link BlobAdapter} using a {@link BlockBlobClient}.
 */
public class DefaultBlobAdapter implements BlobAdapter {
    private final long blockSizeInMb;
    private final int maxConcurrency;
    private final long maxSingleUploadSizeInMb;
    private final BlockBlobClient client;

    public DefaultBlobAdapter(
            BlockBlobClient client, Long blockSizeInMb, Integer maxConcurrency, Long maxSingleUploadSizeInMb) {
        this.blockSizeInMb = blockSizeInMb;
        this.maxConcurrency = maxConcurrency;
        this.maxSingleUploadSizeInMb = maxSingleUploadSizeInMb;
        this.client = client;
    }

    @Override
    public OutputStream getOutputStream() {
        return getOutputStream(null);
    }

    @Override
    public OutputStream getOutputStream(ProgressListener progressListener) {
        var parallelTransferOptions = new ParallelTransferOptions()
                .setBlockSizeLong(blockSizeInMb * Constants.MB)
                .setMaxConcurrency(maxConcurrency)
                .setMaxSingleUploadSizeLong(maxSingleUploadSizeInMb * Constants.MB)
                .setProgressListener(progressListener);

        return client.getBlobOutputStream(new BlockBlobOutputStreamOptions().setParallelTransferOptions(parallelTransferOptions));
    }

    @Override
    public InputStream openInputStream() {
        return client.openInputStream();
    }

    @Override
    public String getBlobName() {
        return client.getBlobName();
    }

    @Override
    public long getBlobSize() {
        return client.getProperties().getBlobSize();
    }

    @Override
    public void setMetadata(Map<String, String> metadata) {
        client.setMetadata(metadata);
    }
}
