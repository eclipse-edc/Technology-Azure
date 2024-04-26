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

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import com.azure.core.credential.AzureSasCredential;
import org.eclipse.edc.azure.blob.adapter.BlobAdapter;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.azure.storage.DestinationBlobName;
import org.eclipse.edc.connector.dataplane.azure.storage.metadata.BlobMetadataProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Writes data into an Azure storage container.
 */
public class AzureStorageDataSink extends ParallelSink {
    // Name of the empty blob used to indicate completion. Used by consumer-side status checker.
    public static final String COMPLETE_BLOB_NAME = ".complete";
    private final List<String> completedFiles = new ArrayList<>();
    private String accountName;
    private String containerName;
    private String sharedAccessSignature;
    private BlobStoreApi blobStoreApi;
    private DataFlowStartMessage request;
    private BlobMetadataProvider metadataProvider;
    private DestinationBlobName destinationBlobName;

    private AzureStorageDataSink() {
    }

    void registerCompletedFile(String name) {
        completedFiles.add(name + COMPLETE_BLOB_NAME);
    }


    /**
     * Writes data into an Azure storage container.
     */

    @Override
    protected StreamResult<Object> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            var name = destinationBlobName.resolve(part.name(), parts.size());
            try (var input = part.openStream()) {
                try (var output = getAdapter(name).getOutputStream()) {
                    try {
                        input.transferTo(output);
                    } catch (Exception e) {
                        return getTransferResult(e, "Error transferring blob for %s on account %s", name, accountName);
                    }
                } catch (Exception e) {
                    return getTransferResult(e, "Error creating blob %s on account %s", name, accountName);
                }
            } catch (Exception e) {
                return getTransferResult(e, "Error reading blob %s", name);
            }
            try {
                getAdapter(name).setMetadata(metadataProvider.provideSinkMetadata(request, part).getMetadata());
            } catch (Exception e) {
                return getTransferResult(e, "Error updating metadata for blob : %s", name);
            }
            registerCompletedFile(name);
        }
        return StreamResult.success();
    }


    @Override
    protected StreamResult<Object> complete() {
        for (var completedFile : completedFiles) {
            try {
                // Write an empty blob to indicate completion
                getAdapter(completedFile).getOutputStream().close();
            } catch (Exception e) {
                return getTransferResult(e, "Error creating blob %s on account %s", completedFile, accountName);
            }
            monitor.info(String.format("Created empty blob '%s' to indicate transfer success", completedFile));
        }
        return super.complete();
    }

    protected BlobAdapter getAdapter(String blobName) {
        return blobStoreApi.getBlobAdapter(accountName, containerName, blobName, new AzureSasCredential(sharedAccessSignature));
    }

    @NotNull
    private StreamResult<Object> getTransferResult(Exception e, String logMessage, Object... args) {
        var message = format(logMessage, args);
        monitor.severe(message, e);
        return StreamResult.error(message);
    }

    public static class Builder extends ParallelSink.Builder<Builder, AzureStorageDataSink> {

        private Builder() {
            super(new AzureStorageDataSink());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder accountName(String accountName) {
            sink.accountName = accountName;
            return this;
        }

        public Builder containerName(String containerName) {
            sink.containerName = containerName;
            return this;
        }

        public Builder sharedAccessSignature(String sharedAccessSignature) {
            sink.sharedAccessSignature = sharedAccessSignature;
            return this;
        }

        public Builder blobStoreApi(BlobStoreApi blobStoreApi) {
            sink.blobStoreApi = blobStoreApi;
            return this;
        }

        public Builder request(DataFlowStartMessage request) {
            sink.request = request;
            return this;
        }

        public Builder metadataProvider(BlobMetadataProvider metadataProvider) {
            sink.metadataProvider = metadataProvider;
            return this;
        }

        public Builder destinationBlobName(DestinationBlobName destinationBlobName) {
            sink.destinationBlobName = destinationBlobName;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.accountName, "accountName");
            Objects.requireNonNull(sink.containerName, "containerName");
            Objects.requireNonNull(sink.sharedAccessSignature, "sharedAccessSignature");
            Objects.requireNonNull(sink.blobStoreApi, "blobStoreApi");
            Objects.requireNonNull(sink.metadataProvider, "metadataProvider");
            Objects.requireNonNull(sink.request, "request");
            Objects.requireNonNull(sink.monitor, "monitor");
        }
    }
}