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
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.TestFunctions;
import org.eclipse.edc.connector.store.sql.contractdefinition.SqlContractDefinitionStore;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@ParallelPostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
class PostgresContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {

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

        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        sqlContractDefinitionStore = new SqlContractDefinitionStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, SQL_STATEMENTS, typeManager.getMapper(), queryExecutor);
        helper.truncateTable(SQL_STATEMENTS.getContractDefinitionTable());
    }

    @Test
    @DisplayName("Verify empty result when query contains invalid keys")
    void findAll_queryByInvalidKey() {

        var definitionsExpected = TestFunctions.createContractDefinitions(20);
        saveContractDefinitions(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("notexist", "=", "somevalue")))
                .build();

        assertThatThrownBy(() -> getContractDefinitionStore().findAll(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    // Override in PG since it does not have the field mapping
    @Test
    void findAll_verifySorting_invalidProperty() {
        range(0, 10).mapToObj(i -> TestFunctions.createContractDefinition("id" + i)).forEach(getContractDefinitionStore()::save);
        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThatThrownBy(() -> getContractDefinitionStore().findAll(query)).isInstanceOf(IllegalArgumentException.class);
    }

    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return sqlContractDefinitionStore;
    }

    protected boolean supportsCollectionQuery() {
        return true;
    }

    protected boolean supportsCollectionIndexQuery() {
        return false;
    }

}
