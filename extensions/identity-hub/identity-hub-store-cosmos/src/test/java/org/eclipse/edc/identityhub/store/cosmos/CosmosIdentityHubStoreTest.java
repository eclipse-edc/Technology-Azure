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

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStoreTestBase;
import org.eclipse.edc.identityhub.store.sql.SqlIdentityHubStore;
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@ParallelPostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
public class CosmosIdentityHubStoreTest extends IdentityHubStoreTestBase {
    public static final BaseSqlIdentityHubStatements STATEMENTS = new BaseSqlIdentityHubStatements();
    private SqlIdentityHubStore store;

    @BeforeAll
    static void createDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getTable());
    }

    @BeforeEach
    void setUp(TransactionContext transactionContext, DataSourceRegistry reg, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper) {
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();

        store = new SqlIdentityHubStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, statements, typeManager.getMapper(), queryExecutor);
        helper.truncateTable(STATEMENTS.getTable());
    }

    @Override
    protected IdentityHubStore getStore() {
        return store;
    }

}
