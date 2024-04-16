/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.store.azure.cosmos.policydefinition;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.controlplane.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
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
class CosmosPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {

    private static final PostgresDialectStatements STATEMENTS = new PostgresDialectStatements();
    private SqlPolicyDefinitionStore sqlPolicyStore;

    @BeforeAll
    static void prepare(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getPolicyTable());
    }

    @BeforeEach
    void setUp(TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper, DataSourceRegistry reg) {
        var manager = new JacksonTypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlPolicyStore = new SqlPolicyDefinitionStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, manager.getMapper(), STATEMENTS, queryExecutor);

        helper.truncateTable(STATEMENTS.getPolicyTable());
    }

    @Override
    protected SqlPolicyDefinitionStore getPolicyDefinitionStore() {
        return sqlPolicyStore;
    }

}
