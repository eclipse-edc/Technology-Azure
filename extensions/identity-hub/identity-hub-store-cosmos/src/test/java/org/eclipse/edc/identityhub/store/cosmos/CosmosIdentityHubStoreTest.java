/*
 *  Copyright (c) 2020 - 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.cosmos;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStoreTestBase;
import org.eclipse.edc.identityhub.store.sql.SqlIdentityHubStore;
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;
import java.sql.SQLException;

@AzureCosmosDbIntegrationTest
@ExtendWith(EdcExtension.class)
public class CosmosIdentityHubStoreTest extends IdentityHubStoreTestBase {
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlIdentityHubStore store;
    private DataSource dataSource;
    private NoopTransactionContext transactionContext;

    @BeforeEach
    void setUp() {
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();
        dataSource = CosmosPostgresFunctions.createDataSource();
        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        reg.register(dsName, dataSource);

        transactionContext = new NoopTransactionContext();

        store = new SqlIdentityHubStore(reg, dsName, transactionContext, statements, typeManager.getMapper(), queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("schema.sql");
        runQuery(schema);
    }

    @AfterEach
    void tearDown() {
        var dialect = new BaseSqlIdentityHubStatements();
        runQuery("DROP TABLE " + dialect.getTable());
    }

    @Override
    protected IdentityHubStore getStore() {
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
