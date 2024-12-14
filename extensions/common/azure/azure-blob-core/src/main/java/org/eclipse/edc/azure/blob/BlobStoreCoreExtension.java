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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.azure.blob;

import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.azure.blob.api.BlobStoreApiImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides(BlobStoreApi.class)
@Extension(value = BlobStoreCoreExtension.NAME)
public class BlobStoreCoreExtension implements ServiceExtension {

    @Setting
    public static final String EDC_AZURE_BLOCK_SIZE_MB = "edc.azure.block.size.mb";
    public static final long EDC_AZURE_BLOCK_SIZE_MB_DEFAULT = 4L;

    @Setting
    public static final String EDC_AZURE_MAX_CONCURRENCY = "edc.azure.max.concurrency";
    public static final int EDC_AZURE_MAX_CONCURRENCY_DEFAULT = 2;

    @Setting
    public static final String EDC_AZURE_MAX_SINGLE_UPLOAD_SIZE_MB = "edc.azure.max.single.upload.size.mb";
    public static final long EDC_AZURE_MAX_SINGLE_UPLOAD_SIZE_MB_DEFAULT = 60L;

    @Setting
    public static final String EDC_BLOBSTORE_ENDPOINT_TEMPLATE = "edc.blobstore.endpoint.template";
    public static final String EDC_BLOBSTORE_ENDPOINT_TEMPLATE_DEFAULT = "https://%s.blob.core.windows.net";

    public static final String NAME = "Azure BlobStore Core";

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var blockSizeInMb = context.getConfig().getLong(EDC_AZURE_BLOCK_SIZE_MB, EDC_AZURE_BLOCK_SIZE_MB_DEFAULT);
        var maxConcurrency = context.getConfig().getInteger(EDC_AZURE_MAX_CONCURRENCY, EDC_AZURE_MAX_CONCURRENCY_DEFAULT);
        var maxSingleUploadSizeInMb = context.getConfig().getLong(EDC_AZURE_MAX_SINGLE_UPLOAD_SIZE_MB, EDC_AZURE_MAX_SINGLE_UPLOAD_SIZE_MB_DEFAULT);
        var blobstoreEndpointTemplate = context
                .getSetting(EDC_BLOBSTORE_ENDPOINT_TEMPLATE, EDC_BLOBSTORE_ENDPOINT_TEMPLATE_DEFAULT);

        var blobStoreApi = new BlobStoreApiImpl(
                vault, blobstoreEndpointTemplate, blockSizeInMb, maxConcurrency, maxSingleUploadSizeInMb);
        context.registerService(BlobStoreApi.class, blobStoreApi);
    }
}
