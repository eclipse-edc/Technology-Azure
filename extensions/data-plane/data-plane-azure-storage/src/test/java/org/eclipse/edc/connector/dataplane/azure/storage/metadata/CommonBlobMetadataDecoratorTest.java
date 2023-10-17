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
 *       Robert Bosch GmbH - initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage.metadata;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommonBlobMetadataDecoratorTest {

    private final TypeManager typeManager = mock();
    private final DataFlowRequest.Builder requestBuilder = createRequest(AzureBlobStoreSchema.TYPE);

    @ParameterizedTest
    @CsvSource(value = {"correlation-id", "''", "null"}, nullValues = {"null"})
    void decorate_succeeds(String correlationId) {

        var context = mock(ServiceExtensionContext.class);
        var part = mock(DataSource.Part.class);

        when(part.name()).thenReturn("original-name");

        when(context.getConnectorId()).thenReturn("connector-id");
        when(context.getParticipantId()).thenReturn("participant-id");

        var decorator = new CommonBlobMetadataDecorator(typeManager, context);
        var builder = mock(BlobMetadata.Builder.class);
        when(builder.put(anyString(), anyString())).thenReturn(builder);

        DataFlowRequest request;
        if (correlationId != null) {
            request = requestBuilder.destinationDataAddress(
                    DataAddress.Builder.newInstance()
                            .type(AzureBlobStoreSchema.TYPE)
                            .property(AzureBlobStoreSchema.CORRELATION_ID, correlationId)
                            .build()).build();
        } else {
            request = requestBuilder.build();
        }

        var result = decorator.decorate(request, part, builder);

        verify(builder).put("originalName", "original-name");

        verify(builder).put("requestId", request.getId());
        verify(builder).put("processId", request.getProcessId());

        verify(builder).put("connectorId", "connector-id");
        verify(builder).put("participantId", "participant-id");

        if (correlationId != null && !correlationId.isEmpty()) {
            verify(builder).put("correlationId", correlationId);
        }

        assertThat(result).isInstanceOf(BlobMetadata.Builder.class);
    }

    @Test
    void decorate_fails() {

        var context = mock(ServiceExtensionContext.class);
        var part = mock(DataSource.Part.class); // Close is no-op

        when(part.name()).thenReturn("ä§");

        var decorator = new CommonBlobMetadataDecorator(typeManager, context);
        var builder = mock(BlobMetadata.Builder.class);
        when(builder.put(anyString(), anyString())).thenReturn(builder);
        var request = requestBuilder.build();
        assertThrows(
                NullPointerException.class,
                () -> decorator.decorate(request, part, builder),
                "Expected decorate to throw NullPointerException, but it didn't"
        );
    }
}
