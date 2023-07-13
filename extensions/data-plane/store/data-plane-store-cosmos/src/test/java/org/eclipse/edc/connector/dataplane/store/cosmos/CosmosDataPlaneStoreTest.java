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

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase;
import org.eclipse.edc.connector.dataplane.store.sql.SqlDataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresDataPlaneStatements;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;

import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@ParallelPostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
public class CosmosDataPlaneStoreTest extends DataPlaneStoreTestBase {

    private static final PostgresDataPlaneStatements STATEMENTS = new PostgresDataPlaneStatements();
    private final Clock clock = Clock.systemUTC();
    private SqlDataPlaneStore store;

    @BeforeAll
    static void setupDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getDataPlaneTable());
    }

    @BeforeEach
    void setUp(TransactionContext transactionContext, QueryExecutor queryExecutor, DataSourceRegistry reg, CosmosPostgresTestExtension.SqlHelper helper) {
        var typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstance.class);
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlDataPlaneStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, STATEMENTS, typeManager.getMapper(), clock, queryExecutor);
        helper.truncateTable(STATEMENTS.getDataPlaneTable());
    }

    @Override
    protected DataPlaneStore getStore() {
        return store;
    }

}
