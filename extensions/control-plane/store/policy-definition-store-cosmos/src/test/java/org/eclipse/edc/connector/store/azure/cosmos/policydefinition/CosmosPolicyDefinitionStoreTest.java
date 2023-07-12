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

import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.connector.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions.createDataSource;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPolicy;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPolicyBuilder;
import static org.eclipse.edc.spi.query.Criterion.criterion;

@AzureCosmosDbIntegrationTest
@ExtendWith(EdcExtension.class)
class CosmosPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {

    private final PostgresDialectStatements statements = new PostgresDialectStatements();
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlPolicyDefinitionStore sqlPolicyStore;
    private DataSource dataSource;
    private NoopTransactionContext transactionContext;

    @BeforeEach
    void setUp() {
        var statements = new PostgresDialectStatements();
        var manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        dataSource = createDataSource();
        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        reg.register(dsName, dataSource);

        System.setProperty("edc.datasource.policy.name", dsName);

        transactionContext = new NoopTransactionContext();

        sqlPolicyStore = new SqlPolicyDefinitionStore(reg, dsName, transactionContext, manager.getMapper(), statements, queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("schema.sql");
        runQuery(schema);
    }

    @AfterEach
    void tearDown() {
        runQuery("DROP TABLE " + statements.getPolicyTable() + " CASCADE");
    }

    @Test
    void find_queryByProperty_notExist() {
        var policy = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().id("test-policy").policy(policy).build();
        getPolicyDefinitionStore().create(policyDef1);

        // query by prohibition assignee
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion("notexist", "=", "foobar")).build();
        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(querySpec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Test
    void findAll_sorting_nonExistentProperty() {

        IntStream.range(0, 10).mapToObj(i -> createPolicy("test-policy")).forEach((d) -> getPolicyDefinitionStore().create(d));
        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();
        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    @Override
    protected SqlPolicyDefinitionStore getPolicyDefinitionStore() {
        return sqlPolicyStore;
    }

    @Override
    protected boolean supportCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportCollectionIndexQuery() {
        return false;
    }

    @Override
    protected Boolean supportSortOrder() {
        return true;
    }

    private void runQuery(String schema) {
        try (var connection = dataSource.getConnection()) {
            transactionContext.execute(() -> queryExecutor.execute(connection, schema));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
