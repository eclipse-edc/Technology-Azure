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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class BlobMetadataProviderImplTest {

    @Test
    public void multiple_succeeds() {

        var monitor = mock(Monitor.class);
        var provider = new BlobMetadataProviderImpl(monitor);
        var requestMock = mock(DataFlowRequest.class);
        var partMock = mock(DataSource.Part.class);
        provider.registerDecorator((request, part, builder) -> {
            assertNotNull(request);
            assertNotNull(part);
            assertNotNull(builder);
            builder.put("key1", "value1");
            return builder;
        });
        provider.registerDecorator((request, part, builder) -> {
            assertNotNull(request);
            assertNotNull(part);
            assertNotNull(builder);
            builder.put("key2", "value2");
            return builder;
        });
        var map = provider.provideSinkMetadata(requestMock, partMock).getMetadata();
        assertThat(map.get("key1")).isEqualTo("value1");
        assertThat(map.get("key2")).isEqualTo("value2");
        assertThat(map.size()).isEqualTo(2);
    }
}
