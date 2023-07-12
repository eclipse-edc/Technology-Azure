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


import org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.edc.connector.contract.spi.testfixtures.offer.store.TestFunctions;
import org.eclipse.edc.connector.store.sql.contractdefinition.SqlContractDefinitionStore;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.store.sql.contractdefinition.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentTest
@ExtendWith(EdcExtension.class)
class PostgresContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {

    private final BaseSqlDialectStatements statements = new PostgresDialectStatements();

    private SqlContractDefinitionStore sqlContractDefinitionStore;
    private DataSource dataSource;
    private TransactionContext transactionContext;
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();

    @BeforeEach
    void setUp() {

        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        dataSource = CosmosPostgresFunctions.createDataSource();
        reg.register(dsName, dataSource);

        System.setProperty("edc.datasource.contractdefinition.name", dsName);

        transactionContext = new NoopTransactionContext();
        sqlContractDefinitionStore = new SqlContractDefinitionStore(reg, dsName,
                transactionContext, statements, typeManager.getMapper(), queryExecutor);
        var schema = TestUtils.getResourceFileContentAsString("schema.sql");
        runQuery(schema);
    }

    @AfterEach
    void tearDown() {
        runQuery("DROP TABLE " + statements.getContractDefinitionTable() + " CASCADE");
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

    @Override
    protected boolean supportsCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportsCollectionIndexQuery() {
        return false;
    }

    private void runQuery(String schema) {
        try (var connection = dataSource.getConnection()) {
            transactionContext.execute(() -> queryExecutor.execute(connection, schema));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
