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

package org.eclipse.edc.registration.store.cosmos;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.registration.store.cosmos.model.ParticipantDocument;
import org.eclipse.edc.registration.store.spi.ParticipantStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Extension that provides a {@link ParticipantStore} with CosmosDB as backend storage
 */
@Extension(value = CosmosParticipantStoreExtension.NAME)
public class CosmosParticipantStoreExtension implements ServiceExtension {

    public static final String NAME = "Cosmos Participant Store";

    @Inject
    private RetryPolicy<Object> retryPolicy;
    @Inject
    private Vault vault;
    @Inject
    private CosmosClientProvider clientProvider;

    @Inject
    private TypeManager typeManager;

    private CosmosDbApi cosmosDbApi;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));
        typeManager.registerTypes(ParticipantDocument.class);
    }

    @Provider
    public ParticipantStore participantStore(ServiceExtensionContext context) {
        var configuration = new ParticipantStoreCosmosConfig(context);
        var client = clientProvider.createClient(vault, configuration);
        cosmosDbApi = new CosmosDbApiImpl(configuration, client);
        return new CosmosParticipantStore(cosmosDbApi, configuration.getPartitionKey(), typeManager.getMapper(), retryPolicy);
    }
}
