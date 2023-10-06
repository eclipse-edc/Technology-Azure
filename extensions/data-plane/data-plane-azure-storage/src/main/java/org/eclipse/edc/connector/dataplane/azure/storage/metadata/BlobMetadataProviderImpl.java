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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.ArrayList;
import java.util.List;

public class BlobMetadataProviderImpl implements BlobMetadataProvider {

    private final List<BlobMetadataDecorator> sinkDecorators = new ArrayList<>();

    private final Monitor monitor;

    public BlobMetadataProviderImpl(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void registerSinkDecorator(BlobMetadataDecorator decorator) {
        sinkDecorators.add(decorator);
    }

    @Override
    public BlobMetadata provideSinkMetadata(DataFlowRequest request, DataSource.Part part) {
        final var metadata = new BlobMetadata.Builder(monitor);
        sinkDecorators.forEach(decorator -> decorator.decorate(request, part, metadata));
        return metadata.build();
    }
}
