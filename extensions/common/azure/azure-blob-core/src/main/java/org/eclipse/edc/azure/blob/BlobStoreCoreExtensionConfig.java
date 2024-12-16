/*
 *  Copyright (c) 2024 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.azure.blob;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public record BlobStoreCoreExtensionConfig(
        @Setting(key = "edc.azure.block.size.mb", description = "The block size, in mb, to parallel blob upload.", defaultValue = "4", required = false) long blockSize,
        @Setting(key = "edc.azure.max.concurrency", description = "Maximum number of parallel requests in a transfer.", defaultValue = "2", required = false) int maxConcurrency,
        @Setting(key = "edc.azure.max.single.upload.size.mb", description = "Maximum size, in mb, for a single upload.", defaultValue = "60", required = false) long maxSingleUploadSize,
        @Setting(key = "edc.blobstore.endpoint.template", description = "Template for the blob service endpoint.", defaultValue = "https://%s.blob.core.windows.net", required = false) String blobstoreEndpointTemplate) {
}
