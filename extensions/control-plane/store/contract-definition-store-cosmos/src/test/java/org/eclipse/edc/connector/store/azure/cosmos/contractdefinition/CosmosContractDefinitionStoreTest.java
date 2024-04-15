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

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;


import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.testfixtures.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.SqlContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.controlplane.store.sql.contractdefinition.schema.postgres.PostgresDialectStatements;
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
class CosmosContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {

    private static final BaseSqlDialectStatements SQL_STATEMENTS = new PostgresDialectStatements();
    private SqlContractDefinitionStore sqlContractDefinitionStore;

    @BeforeAll
    static void prepare(CosmosPostgresTestExtension.SqlHelper runner) {
        runner.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper runner) {
        runner.dropTable(SQL_STATEMENTS.getContractDefinitionTable());
    }

    @BeforeEach
    void setUp(TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper, DataSourceRegistry reg) {
        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlContractDefinitionStore = new SqlContractDefinitionStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, SQL_STATEMENTS, typeManager.getMapper(), queryExecutor);
        helper.truncateTable(SQL_STATEMENTS.getContractDefinitionTable());
    }

    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return sqlContractDefinitionStore;
    }

}
