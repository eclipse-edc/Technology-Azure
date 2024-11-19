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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * {@link TransferService} implementation that performs transfers in Azure Data Factory.
 */
public class AzureDataFactoryTransferService implements TransferService {
    private final AzureDataFactoryTransferRequestValidator validator;
    private final AzureDataFactoryTransferManager transferManager;

    public AzureDataFactoryTransferService(AzureDataFactoryTransferRequestValidator validator, AzureDataFactoryTransferManager transferManager) {
        this.validator = validator;
        this.transferManager = transferManager;
    }

    @Override
    public boolean canHandle(DataFlowStartMessage request) {
        return validator.canHandle(request);
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowStartMessage request) {
        return validator.validate(request);
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataFlowStartMessage request) {
        return transferManager.transfer(request);
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataFlowStartMessage request, DataSink sink) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public StreamResult<Void> terminate(DataFlow dataFlow) {
        return StreamResult.success();
    }

    @Override
    public void closeAll() {

    }
}
