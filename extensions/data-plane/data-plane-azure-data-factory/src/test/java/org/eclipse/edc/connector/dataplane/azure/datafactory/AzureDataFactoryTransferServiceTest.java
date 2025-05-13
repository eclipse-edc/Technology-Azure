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

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureDataFactoryTransferServiceTest {

    private final AzureDataFactoryTransferRequestValidator validator = mock();
    private final AzureDataFactoryTransferManager transferManager = mock();
    private final AzureDataFactoryTransferService transferService = new AzureDataFactoryTransferService(
            validator,
            transferManager);

    private final DataFlowStartMessage.Builder request = createRequest(AzureBlobStoreSchema.TYPE);

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void canHandle_onResult(boolean expected) {
        when(validator.canHandle(request.build())).thenReturn(expected);

        assertThat(transferService.canHandle(request.build())).isEqualTo(expected);
    }

    @Test
    void validate_onSuccess() {
        var success = Result.success(true);
        when(validator.validate(request.build())).thenReturn(success);

        var result = transferService.validate(request.build());

        assertThat(result).isSucceeded();
    }

    @Test
    void validate_onFailure() {
        var failure = Result.<Boolean>failure("Test Failure");
        when(validator.validate(request.build())).thenReturn(failure);

        var result = transferService.validate(request.build());

        assertThat(result).isFailed().detail().isEqualTo("Test Failure");
    }

    @Test
    void transfer() {
        var future = new CompletableFuture<StreamResult<Object>>();
        when(transferManager.transfer(request.build())).thenReturn(future);

        var result = transferService.transfer(request.build());

        assertThat(result).isSameAs(future);
    }
}
