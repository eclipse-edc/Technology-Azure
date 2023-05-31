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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.RetryPolicy;
import dev.failsafe.function.CheckedRunnable;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.azure.cosmos.dialect.SqlStatement;
import org.eclipse.edc.registration.spi.model.Participant;
import org.eclipse.edc.registration.spi.model.ParticipantStatus;
import org.eclipse.edc.registration.store.cosmos.model.ParticipantDocument;
import org.eclipse.edc.registration.store.spi.ParticipantStore;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static dev.failsafe.Failsafe.with;

/**
 * CosmosDB implementation for {@link ParticipantStore}
 */
public class CosmosParticipantStore implements ParticipantStore {

    public static final String DID_FIELD = "did";
    public static final String EQ = "=";
    public static final String STATE_FIELD = "state";
    private final CosmosDbApi participantDb;
    private final String partitionKey;
    private final ObjectMapper objectMapper;
    private final RetryPolicy<Object> retryPolicy;


    public CosmosParticipantStore(CosmosDbApi participantDb, String partitionKey, ObjectMapper objectMapper, RetryPolicy<Object> retryPolicy) {
        this.participantDb = Objects.requireNonNull(participantDb);
        this.partitionKey = partitionKey;
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
    }

    @Override
    public @Nullable Participant findByDid(String did) {
        var query = new SqlStatement<>(ParticipantDocument.class)
                .where(List.of(new Criterion(DID_FIELD, EQ, did)))
                .getQueryAsSqlQuerySpec();
        return with(retryPolicy).get(() -> participantDb.queryItems(query))
                .findFirst()
                .map(this::convertObject)
                .map(ParticipantDocument::getWrappedInstance)
                .orElse(null);
    }

    @Override
    public List<Participant> listParticipants() {
        return with(retryPolicy).get(() -> participantDb.queryAllItems())
                .stream()
                .map(this::convertObject)
                .map(ParticipantDocument::getWrappedInstance)
                .collect(Collectors.toList());
    }

    @Override
    public StoreResult<Participant> save(Participant participant) {
        var document = new ParticipantDocument(participant, partitionKey);
        CheckedRunnable action;
        if (findByDid(participant.getDid()) == null) {
            action = () -> participantDb.createItem(document);
        } else {
            action = () -> participantDb.updateItem(document);
        }
        with(retryPolicy).run(action);
        return StoreResult.success(participant);
    }

    @Override
    public Collection<Participant> listParticipantsWithStatus(ParticipantStatus state) {
        var query = new SqlStatement<>(ParticipantDocument.class)
                .where(List.of(new Criterion(STATE_FIELD, EQ, state.code())))
                .getQueryAsSqlQuerySpec();
        return with(retryPolicy).get(() -> participantDb.queryItems(query))
                .map(this::convertObject)
                .map(ParticipantDocument::getWrappedInstance)
                .collect(Collectors.toList());
    }


    private ParticipantDocument convertObject(Object databaseDocument) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(databaseDocument), ParticipantDocument.class);
        } catch (JsonProcessingException e) {
            throw new EdcPersistenceException(e);
        }
    }

}
