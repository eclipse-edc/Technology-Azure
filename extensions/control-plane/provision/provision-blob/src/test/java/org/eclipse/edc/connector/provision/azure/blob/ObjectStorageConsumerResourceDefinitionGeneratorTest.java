/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       ZF Friedrichshafen AG - Addition of new tests
 *       SAP SE - refactoring
 *
 */

package org.eclipse.edc.connector.provision.azure.blob;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObjectStorageConsumerResourceDefinitionGeneratorTest {

    private final TransferTypeParser transferTypeParser = mock();
    private final ObjectStorageConsumerResourceDefinitionGenerator generator =
            new ObjectStorageConsumerResourceDefinitionGenerator(transferTypeParser);

    @Nested
    class Generate {

        @Test
        void generate_withContainerName() {
            var destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                    .property(AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                    .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                    .build();
            var asset = Asset.Builder.newInstance().build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(destination).assetId(asset.getId())
                    .build();

            var definition = generator.generate(transferProcess, emptyPolicy());

            assertThat(definition).isInstanceOf(ObjectStorageResourceDefinition.class);
            var objectDef = (ObjectStorageResourceDefinition) definition;
            assertThat(objectDef.getAccountName()).isEqualTo("test-account");
            assertThat(objectDef.getContainerName()).isEqualTo("test-container");
            assertThat(objectDef.getId()).satisfies(UUID::fromString);
            assertThat(objectDef.getFolderName()).isNull();
            assertThat(objectDef.getBlobName()).isNull();
        }

        @Test
        void generate_withContainerName_andFolder_andBlobName() {
            var destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                    .property(AzureBlobStoreSchema.CONTAINER_NAME, "test-container")
                    .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                    .property(AzureBlobStoreSchema.FOLDER_NAME, "test-folder")
                    .property(AzureBlobStoreSchema.BLOB_NAME, "test-blob")
                    .build();
            var asset = Asset.Builder.newInstance().build();
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("%s-%s".formatted(AzureBlobStoreSchema.TYPE, FlowType.PUSH))
                    .dataDestination(destination)
                    .assetId(asset.getId())
                    .build();

            var definition = generator.generate(transferProcess, emptyPolicy());

            assertThat(definition).isNotNull().isInstanceOf(ObjectStorageResourceDefinition.class);
            var objectDef = (ObjectStorageResourceDefinition) definition;
            assertThat(objectDef.getAccountName()).isEqualTo("test-account");
            assertThat(objectDef.getContainerName()).isEqualTo("test-container");
            assertThat(objectDef.getId()).satisfies(UUID::fromString);
            assertThat(objectDef.getFolderName()).isEqualTo("test-folder");
            assertThat(objectDef.getBlobName()).isEqualTo("test-blob");
        }

        @Test
        void generate_withoutContainerName() {
            var destination = DataAddress.Builder.newInstance().type(AzureBlobStoreSchema.TYPE)
                    .property(AzureBlobStoreSchema.ACCOUNT_NAME, "test-account")
                    .build();
            var asset = Asset.Builder.newInstance().build();
            var transferProcess = TransferProcess.Builder.newInstance().dataDestination(destination).assetId(asset.getId()).build();

            var definition = generator.generate(transferProcess, emptyPolicy());

            assertThat(definition).isInstanceOf(ObjectStorageResourceDefinition.class);
            var objectDef = (ObjectStorageResourceDefinition) definition;
            assertThat(objectDef.getAccountName()).isEqualTo("test-account");
            assertThat(objectDef.getContainerName()).satisfies(UUID::fromString);
            assertThat(objectDef.getId()).satisfies(UUID::fromString);
        }

        @Test
        void shouldCreateEmptyDefinition_whenDestinationIsNull() {
            var transferProcess = TransferProcess.Builder.newInstance()
                    .dataDestination(null)
                    .assetId(UUID.randomUUID().toString())
                    .build();

            var definition = generator.generate(transferProcess, emptyPolicy());

            assertThat(definition).isInstanceOfSatisfying(ObjectStorageResourceDefinition.class, def -> {
                assertThat(def.getAccountName()).satisfies(UUID::fromString);
                assertThat(def.getContainerName()).satisfies(UUID::fromString);
                assertThat(def.getId()).satisfies(UUID::fromString);
            });

        }
    }

    @Nested
    class CanGenerate {

        @Test
        void shouldReturnTrue_whenDestinationTypeIsTheExpectedOne() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType(AzureBlobStoreSchema.TYPE, FlowType.PULL)));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("valid transfer type")
                    .assetId(UUID.randomUUID().toString())
                    .build();

            var definition = generator.canGenerate(transferProcess, emptyPolicy());

            assertThat(definition).isTrue();
        }

        @Test
        void shouldReturnFalse_whenDestinationTypeIsNotTheExpectedOne() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("another type", FlowType.PULL)));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("another transfer type")
                    .assetId(UUID.randomUUID().toString())
                    .build();

            var definition = generator.canGenerate(transferProcess, emptyPolicy());

            assertThat(definition).isFalse();
        }

        @Test
        void shouldReturnFalse_whenTransferTypeCannotBeParsedCorrectly() {
            when(transferTypeParser.parse(any())).thenReturn(Result.failure("transfer type cannot be parsed"));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("another transfer type")
                    .assetId(UUID.randomUUID().toString())
                    .build();

            var definition = generator.canGenerate(transferProcess, emptyPolicy());

            assertThat(definition).isFalse();
        }

    }

    private Policy emptyPolicy() {
        return Policy.Builder.newInstance().build();
    }

}
