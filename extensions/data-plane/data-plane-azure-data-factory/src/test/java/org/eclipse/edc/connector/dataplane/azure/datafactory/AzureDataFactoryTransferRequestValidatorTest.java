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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createDataAddress;
import static org.eclipse.edc.azure.blob.testfixtures.AzureStorageTestFixtures.createRequest;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AzureDataFactoryTransferRequestValidatorTest {
    private static final DataFlowStartMessage.Builder REQUEST = createRequest(AzureBlobStoreSchema.TYPE);

    private final Map<String, Object> sourceProperties = TestFunctions.sourceProperties();
    private final Map<String, Object> destinationProperties = TestFunctions.destinationProperties();
    private final DataAddress.Builder source = createDataAddress(AzureBlobStoreSchema.TYPE);
    private final DataAddress.Builder destination = createDataAddress(AzureBlobStoreSchema.TYPE);
    AzureDataFactoryTransferRequestValidator validator = new AzureDataFactoryTransferRequestValidator();

    @ParameterizedTest(name = "{index} {0}")
    @ArgumentsSource(CanHandle.class)
    void canHandle_onResult(String ignoredName, String sourceType, String destinationType, boolean expected) {
        // Arrange
        var source = createDataAddress(sourceType);
        var destination = createDataAddress(destinationType);
        var request = DataFlowStartMessage.Builder.newInstance()
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(source.build())
                .destinationDataAddress(destination.build());
        // Act & Assert
        assertThat(validator.canHandle(request.build())).isEqualTo(expected);
    }

    @Test
    void validate_whenRequestValid_succeeds() {
        var result = validator.validate(REQUEST
                .sourceDataAddress(source.properties(TestFunctions.sourceProperties()).build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build());
        assertThat(result.succeeded()).withFailMessage(result::getFailureDetail).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidSourceAddress.class)
    void validate_whenMissingSourceProperty_fails(DataAddress.Builder<?, ?> dataAddressBuilder) {
        var request = REQUEST
                .sourceDataAddress(dataAddressBuilder.build())
                .destinationDataAddress(destination.properties(destinationProperties).build())
                .build();

        var result = validator.validate(request);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidDestinationAddress.class)
    void validate_whenMissingDestinationProperty_fails(DataAddress.Builder<?, ?> dataAddressBuilder) {
        assertThat(validator.validate(REQUEST
                .sourceDataAddress(source.properties(sourceProperties).build())
                .destinationDataAddress(dataAddressBuilder.build())
                .build()).failed()).isTrue();
    }

    private static class InvalidSourceAddress implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    dataAddress().property(ACCOUNT_NAME, "accountName").property(CONTAINER_NAME, "containerName").property(BLOB_NAME, "blobName"),
                    dataAddress().keyName("keyName").property(CONTAINER_NAME, "containerName").property(BLOB_NAME, "blobName"),
                    dataAddress().keyName("keyName").property(ACCOUNT_NAME, "accountName").property(BLOB_NAME, "blobName"),
                    dataAddress().keyName("keyName").property(ACCOUNT_NAME, "accountName").property(CONTAINER_NAME, "containerName")
            ).map(Arguments::arguments);
        }

        private DataAddress.Builder<?, ?> dataAddress() {
            return DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE);
        }
    }

    private static class InvalidDestinationAddress implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    dataAddress().property(ACCOUNT_NAME, "accountName").property(CONTAINER_NAME, "containerName"),
                    dataAddress().keyName("keyName").property(CONTAINER_NAME, "containerName"),
                    dataAddress().keyName("keyName").property(ACCOUNT_NAME, "accountName")
            ).map(Arguments::arguments);
        }

        private DataAddress.Builder<?, ?> dataAddress() {
            return DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE);
        }
    }

    private static class CanHandle implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments("Invalid source and valid destination", "Invalid source", AzureBlobStoreSchema.TYPE, false),
                    arguments("Valid source and invalid destination", AzureBlobStoreSchema.TYPE, "Invalid destination", false),
                    arguments("Invalid source and destination", "Invalid source", "Invalid destination", false),
                    arguments("Valid source and destination", AzureBlobStoreSchema.TYPE, AzureBlobStoreSchema.TYPE, true)
            );
        }
    }

}
