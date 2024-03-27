/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.azure.event;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherAsyncClient;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

class AzureEventGridPublisher implements TransferProcessListener {

    private final Monitor monitor;
    private final EventGridPublisherAsyncClient<EventGridEvent> client;
    private final String eventTypeTransferprocess = "dataspaceconnector/transfer/transferprocess";
    private final String connectorId;

    AzureEventGridPublisher(String connectorId, Monitor monitor, EventGridPublisherAsyncClient<EventGridEvent> client) {
        this.connectorId = connectorId;
        this.monitor = monitor;
        this.client = client;
    }

    @Override
    public void preCreated(TransferProcess process) {
        var dto = createTransferProcessDto(process);
        if (process.getType() == TransferProcess.Type.CONSUMER) {
            sendEvent("createdConsumer", eventTypeTransferprocess, dto).subscribe(new LoggingSubscriber<>("Transfer process created"));
        } else {
            sendEvent("createdProvider", eventTypeTransferprocess, dto).subscribe(new LoggingSubscriber<>("Transfer process created"));
        }
    }

    @Override
    public void preCompleted(TransferProcess process) {
        sendEvent("completed", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process completed"));
    }


    @Override
    public void preDeprovisioned(TransferProcess process) {
        sendEvent("deprovisioned", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process resources deprovisioned"));

    }

    @Override
    public void preTerminated(TransferProcess process) {
        sendEvent("ended", eventTypeTransferprocess, createTransferProcessDto(process)).subscribe(new LoggingSubscriber<>("Transfer process ended"));

    }

    private Mono<Void> sendEvent(String what, String where, Object payload) {
        var data = BinaryData.fromObject(payload);
        var evt = new EventGridEvent(what, where, data, "0.1");
        return client.sendEvent(evt);
    }

    @NotNull
    private TransferProcessDto createTransferProcessDto(TransferProcess process) {
        return TransferProcessDto.Builder.newInstance()
                .connector(connectorId)
                .state(TransferProcessStates.from(process.getState()))
                .requestId(process.getCorrelationId())
                .type(process.getType())
                .build();
    }

    private class LoggingSubscriber<T> extends BaseSubscriber<T> {

        private final String message;

        LoggingSubscriber(String message) {
            this.message = message;
        }

        @Override
        protected void hookOnComplete() {
            monitor.debug("AzureEventGrid: " + message);
        }

        @Override
        protected void hookOnError(@NotNull Throwable throwable) {
            monitor.severe("Error during event publishing", throwable);
        }
    }
}
