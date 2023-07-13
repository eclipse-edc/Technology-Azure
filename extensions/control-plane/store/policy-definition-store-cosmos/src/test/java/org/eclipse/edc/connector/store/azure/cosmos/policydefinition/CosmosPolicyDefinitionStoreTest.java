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
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.connector.store.sql.policydefinition.store.SqlPolicyDefinitionStore;
import org.eclipse.edc.connector.store.sql.policydefinition.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPolicy;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPolicyBuilder;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.query.Criterion.criterion;

@ParallelPostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
class CosmosPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {

    private static final PostgresDialectStatements STATEMENTS = new PostgresDialectStatements();
    private SqlPolicyDefinitionStore sqlPolicyStore;

    @BeforeAll
    static void createDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropDatase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getPolicyTable());
    }

    @BeforeEach
    void setUp(DataSource dataSource, TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper, DataSourceRegistry reg) {
        var statements = new PostgresDialectStatements();
        var manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));


        sqlPolicyStore = new SqlPolicyDefinitionStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, manager.getMapper(), statements, queryExecutor);

        helper.truncateTable(STATEMENTS.getPolicyTable());
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
}