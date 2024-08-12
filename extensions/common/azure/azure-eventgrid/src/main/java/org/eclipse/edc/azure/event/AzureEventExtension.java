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

import com.azure.core.credential.AzureKeyCredential;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Objects;

@Extension(value = AzureEventExtension.NAME)
public class AzureEventExtension implements ServiceExtension {

    public static final String NAME = "Azure Events";
    @Setting
    public static final String TOPIC_NAME_SETTING = "edc.events.topic.name";
    @Setting
    public static final String TOPIC_ENDPOINT_SETTING = "edc.events.topic.endpoint";
    public static final String DEFAULT_SYSTEM_TOPIC_NAME = "connector-events";
    public static final String DEFAULT_ENDPOINT_NAME_TEMPLATE = "https://%s.westeurope-1.eventgrid.azure.net/api/events";

    @Inject
    private Monitor monitor;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var topicName = context.getSetting(TOPIC_NAME_SETTING, DEFAULT_SYSTEM_TOPIC_NAME);
        var endpoint = context.getSetting(TOPIC_ENDPOINT_SETTING, DEFAULT_ENDPOINT_NAME_TEMPLATE.formatted(topicName));

        monitor.info("AzureEventExtension: will use topic endpoint " + endpoint);

        var publisherClient = new EventGridPublisherClientBuilder()
                .credential(new AzureKeyCredential(Objects.requireNonNull(vault.resolveSecret(topicName), "Did not find secret in vault: " + endpoint)))
                .endpoint(endpoint)
                .buildEventGridEventPublisherAsyncClient();


        var publisher = new AzureEventGridPublisher(context.getComponentId(), monitor, publisherClient);

        var processObservable = context.getService(TransferProcessObservable.class, true);
        if (processObservable != null) {
            processObservable.registerListener(publisher);
        }
    }

}
