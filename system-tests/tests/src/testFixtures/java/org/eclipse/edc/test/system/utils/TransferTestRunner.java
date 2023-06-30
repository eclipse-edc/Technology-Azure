/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.system.utils;

import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_ASSET_ID;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.PROVIDER_PARTICIPANT_ID;
import static org.eclipse.edc.test.system.utils.Constants.POLL_INTERVAL;
import static org.eclipse.edc.test.system.utils.Constants.TIMEOUT;
import static org.eclipse.edc.test.system.utils.TransferTestClient.getContractId;

/**
 * Orchestrate a transfer test with {@link TransferConfiguration} using the test client {@link TransferTestClient}.
 * It fetches the catalog, for each offer
 */
public class TransferTestRunner {

    private final TransferConfiguration configuration;

    public TransferTestRunner(TransferConfiguration configuration) {
        this.configuration = configuration;
    }

    public void executeTransfer() {
        var client = new TransferTestClient(configuration.getConsumerManagementUrl());

        var dataset = client.getDatasetForAsset(PROVIDER_ASSET_ID, configuration.getProviderIdsUrl());
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = client.negotiateContract(PROVIDER_PARTICIPANT_ID, configuration.getProviderIdsUrl(), contractId.toString(), PROVIDER_ASSET_ID, policy);

        var destination = configuration.createBlobDestination();
        var transferProcessId = client.initiateTransfer(contractAgreementId, PROVIDER_ASSET_ID, configuration.getProviderIdsUrl(), destination);

        await().pollInterval(POLL_INTERVAL)
                .atMost(TIMEOUT)
                .untilAsserted(() -> {
                    var state = client.getTransferProcessState(transferProcessId);
                    // should be STARTED or some state after that to make it more robust.
                    assertThat(TransferProcessStates.valueOf(state).code()).isGreaterThanOrEqualTo(STARTED.code());
                });

        var transferProcess = client.getTransferProcess(transferProcessId);
        assertThat(configuration.isTransferResultValid(transferProcess)).isTrue();
    }
}
