/*
 *  Copyright (c) 2023 Robert Bosch GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Robert Bosch GmbH - adaption for blob metadata
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.metadata;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.edc.util.string.StringUtils;

public class CommonBlobMetadataDecorator implements BlobMetadataDecorator {

    private final TypeManager typeManager;

    private final ServiceExtensionContext context;

    public CommonBlobMetadataDecorator(TypeManager typeManager, ServiceExtensionContext context) {
        this.typeManager = typeManager;
        this.context = context;
    }

    @Override
    public BlobMetadata.Builder decorate(DataFlowRequest request, DataSource.Part part, BlobMetadata.Builder builder) {

        builder.put("originalName", part.name())
                .put("requestId", request.getId())
                .put("processId", request.getProcessId())
                .put("connectorId", context.getConnectorId())
                .put("participantId", context.getParticipantId());

        var dataAddress = request.getDestinationDataAddress();
        var correlationId = dataAddress.getStringProperty(AzureBlobStoreSchema.CORRELATION_ID);

        if (!StringUtils.isNullOrEmpty(correlationId)) {
            builder.put("correlationId", correlationId);
        }

        return builder;
    }
}
