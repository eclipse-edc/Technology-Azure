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

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.registration.spi.model.Participant;
import org.eclipse.edc.registration.store.spi.ParticipantStore;
import org.eclipse.edc.registration.store.spi.ParticipantStoreTestBase;
import org.eclipse.edc.registration.store.sql.SqlParticipantStore;
import org.eclipse.edc.registration.store.sql.schema.ParticipantStatements;
import org.eclipse.edc.registration.store.sql.schema.PostgresSqlParticipantStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.registration.spi.model.ParticipantStatus.AUTHORIZED;
import static org.eclipse.edc.registration.store.spi.TestUtils.createParticipant;

@ExtendWith(CosmosPostgresTestExtension.class)
@ParallelPostgresCosmosTest
class CosmosParticipantStoreIntegrationTest extends ParticipantStoreTestBase {

    private static final ParticipantStatements STATEMENTS = new PostgresSqlParticipantStatements();
    private SqlParticipantStore store;

    @BeforeAll
    static void createDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getParticipantTable());
    }


    @BeforeEach
    void setUp(DataSourceRegistry reg, QueryExecutor queryExecutor, TransactionContext transactionContext, CosmosPostgresTestExtension.SqlHelper helper) {
        var statements = new PostgresSqlParticipantStatements();

        var manager = new JacksonTypeManager();
        manager.registerTypes(Participant.class);

        store = new SqlParticipantStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, manager.getMapper(), statements, queryExecutor);
        helper.truncateTable(STATEMENTS.getParticipantTable());
    }

    @Test
    void saveAndListParticipants_removesDuplicates() {
        var participant1 = createParticipant().did("some.test/url/2").status(AUTHORIZED).build();
        var participant2 = createParticipant().did("some.test/url/2").status(AUTHORIZED).build();

        getStore().save(participant1);

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> getStore().save(participant2))
                .withMessageStartingWith(String.format("Failed to update Participant with did %s", participant2.getDid()));
    }

    @Override
    protected ParticipantStore getStore() {
        return store;
    }
}
