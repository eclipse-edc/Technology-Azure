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

package org.eclipse.edc.connector.dataplane.store.cosmos;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase;
import org.eclipse.edc.connector.dataplane.store.sql.SqlDataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresDataPlaneStatements;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
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
import java.time.Clock;

import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@AzureCosmosDbIntegrationTest
@ExtendWith(EdcExtension.class)
public class CosmosDataPlaneInstanceStoreTest extends DataPlaneStoreTestBase {

    private final Clock clock = Clock.systemUTC();
    private final PostgresDataPlaneStatements statements = new PostgresDataPlaneStatements();
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlDataPlaneStore store;
    private DataSource dataSource;
    private NoopTransactionContext transactionContext;

    @BeforeEach
    void setUp() {

        var typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstance.class);

        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        dataSource = CosmosPostgresFunctions.createDataSource();
        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        reg.register(dsName, dataSource);

        System.setProperty("edc.datasource.contractnegotiation.name", dsName);

        transactionContext = new NoopTransactionContext();

        store = new SqlDataPlaneStore(reg, dsName, transactionContext, statements, typeManager.getMapper(), clock, queryExecutor);
        var schema = getResourceFileContentAsString("schema.sql");
        runQuery(schema);
    }

    @AfterEach
    void tearDown() {
        runQuery("DROP TABLE " + statements.getDataPlaneTable() + " CASCADE");
    }

    @Override
    protected DataPlaneStore getStore() {
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
