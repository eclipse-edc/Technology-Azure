/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSinkFactory;
import org.eclipse.edc.connector.dataplane.azure.storage.pipeline.AzureStorageDataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneAzureStorageExtensionTest {

    private final PipelineService pipelineService = mock();
    private final PublicEndpointGeneratorService generatorService = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(PipelineService.class, pipelineService);
        context.registerService(PublicEndpointGeneratorService.class, generatorService);
    }

    @Test
    void shouldProvidePipelineServices(DataPlaneAzureStorageExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(pipelineService).registerFactory(isA(AzureStorageDataSourceFactory.class));
        verify(pipelineService).registerFactory(isA(AzureStorageDataSinkFactory.class));
    }

    @Test
    void shouldInvokePublicEndpointGeneratorService(DataPlaneAzureStorageExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(generatorService).addGeneratorFunction(eq(AzureBlobStoreSchema.TYPE), isA(Function.class));
    }

}