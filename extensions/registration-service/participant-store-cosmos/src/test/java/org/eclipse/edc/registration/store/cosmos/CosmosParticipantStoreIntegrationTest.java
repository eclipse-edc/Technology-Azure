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

import org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.registration.spi.model.Participant;
import org.eclipse.edc.registration.store.spi.ParticipantStore;
import org.eclipse.edc.registration.store.spi.ParticipantStoreTestBase;
import org.eclipse.edc.registration.store.sql.SqlParticipantStore;
import org.eclipse.edc.registration.store.sql.schema.PostgresSqlParticipantStatements;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.registration.spi.model.ParticipantStatus.AUTHORIZED;
import static org.eclipse.edc.registration.store.spi.TestUtils.createParticipant;

@AzureCosmosDbIntegrationTest
class CosmosParticipantStoreIntegrationTest extends ParticipantStoreTestBase {

    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlParticipantStore store;
    private DataSource dataSource;
    private TransactionContext transactionContext = new NoopTransactionContext();

    @BeforeEach
    void setUp() {
        var statements = new PostgresSqlParticipantStatements();

        var manager = new TypeManager();
        manager.registerTypes(Participant.class);
        dataSource = CosmosPostgresFunctions.createDataSource();
        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        reg.register(dsName, dataSource);

        transactionContext = new NoopTransactionContext();
        store = new SqlParticipantStore(reg, dsName, transactionContext, manager.getMapper(), statements, queryExecutor);

        var schema = getResourceFileContentAsString("schema.sql");
        runQuery(schema);
    }

    @Test
    void saveAndListParticipants_removesDuplicates() {
        var participant1 = createParticipant().did("some.test/url/2").status(AUTHORIZED).build();
        var participant2 = createParticipant().did("some.test/url/2").status(AUTHORIZED).build();

        getStore().save(participant1);

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> getStore().save(participant2))
                .withMessageStartingWith(String.format("Failed to update Participant with did %s", participant2.getDid()));
    }

    @AfterEach
    void tearDown() {
        var dialect = new PostgresSqlParticipantStatements();
        runQuery("DROP TABLE " + dialect.getParticipantTable());
    }

    @Override
    protected ParticipantStore getStore() {
        return store;
    }

    private void runQuery(String schema) {
        try (var connection = dataSource.getConnection()) {
            transactionContext.execute(() -> queryExecutor.execute(connection, schema));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
