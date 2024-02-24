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

import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DataFactoryPipelineFactoryTest {

    private final DataFactoryClient client = mock(DataFactoryClient.class, RETURNS_DEEP_STUBS);
    private final TypeManager typeManager = new TypeManager();
    private final KeyVaultClient keyVaultClient = mock(KeyVaultClient.class);
    private final AzureSasToken azureSasToken = new AzureSasToken("test-wo-sas", new Random().nextLong());
    private final KeyVaultSecret writeOnlySasSecret = new KeyVaultSecret("wo-sas-name", typeManager.writeValueAsString(azureSasToken));
    private final KeyVaultSecret destinationSecret = new KeyVaultSecret("test-secret-name", "test-dest-secret");

    private final DataFlowStartMessage request = TestFunctions.createFlowRequest();

    private final String keyVaultLinkedService = "test-linked-service";
    private final DataFactoryPipelineFactory factory = new DataFactoryPipelineFactory(keyVaultLinkedService, keyVaultClient, client, typeManager);

    @Test
    void createPipeline() {
        when(keyVaultClient.getSecret(request.getDestinationDataAddress().getKeyName()))
                .thenReturn(writeOnlySasSecret);
        when(keyVaultClient.setSecret(any(), eq(azureSasToken.getSas())))
                .thenReturn(destinationSecret);

        factory.createPipeline(request);

        verify(client).definePipeline(any());
        verify(client, times(2)).defineDataset(any());
        verify(client, times(2)).defineLinkedService(any());
        verifyNoMoreInteractions(client);

        verify(keyVaultClient, times(1)).getSecret(any());
        verify(keyVaultClient, times(1)).setSecret(any(), any());
        verifyNoMoreInteractions(keyVaultClient);
    }
}