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

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BlobMetadataTest {

    public void build_succeeds() {
        var monitor = mock(Monitor.class);
        var builder = new BlobMetadata.Builder(monitor);
        builder.put("key1", "value1");
        builder.put("key2", "value2");
        var blobMetadata = builder.build();
        var metadata = blobMetadata.getMetadata();
        assertThat(metadata.get("key1")).isEqualTo("value1");
        assertThat(metadata.get("key2")).isEqualTo("value2");
        assertThat(metadata.size()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"test,#§Ö", "#§Ö,test"})
    public void build_fails(String key, String value) {
        var monitor = mock(Monitor.class);
        var builder = new BlobMetadata.Builder(monitor);
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.put(key, value),
                "Expected decorate to throw IllegalArgumentException, but it didn't");
    }

    @Test
    public void build_indicates_warning() {
        var monitor = mock(Monitor.class);
        var builder = new BlobMetadata.Builder(monitor);
        builder.put("key1", "value1");
        builder.put("key1", "value2");
        var blobMetadata = builder.build();
        var metadata = blobMetadata.getMetadata();
        assertThat(metadata.get("key1")).isEqualTo("value2");
        assertThat(metadata.size()).isEqualTo(1);
        verify(monitor).warning("Overwriting existing metadata key : key1");
    }
}