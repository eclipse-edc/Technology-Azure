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

package org.eclipse.edc.connector.dataplane.selector.store.cosmos;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.store.DataPlaneInstanceStoreTestBase;
import org.eclipse.edc.connector.dataplane.selector.store.sql.SqlDataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.edc.connector.dataplane.selector.store.sql.schema.postgres.PostgresDataPlaneInstanceStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;

import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@ParallelPostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
public class CosmosDataPlaneInstanceStoreTest extends DataPlaneInstanceStoreTestBase {
    private static final DataPlaneInstanceStatements STATEMENTS = new PostgresDataPlaneInstanceStatements();
    private final DataPlaneInstanceStatements statements = new PostgresDataPlaneInstanceStatements();
    private SqlDataPlaneInstanceStore store;
    private LeaseUtil leaseUtil;

    @BeforeAll
    static void createDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getDataPlaneInstanceTable());
    }

    @BeforeEach
    void setUp(DataSourceRegistry reg, DataSource dataSource, TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper) {

        var clock = Clock.systemUTC();

        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(DataPlaneInstance.class);
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        leaseUtil = new LeaseUtil(transactionContext, () -> {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, statements, clock);

        store = new SqlDataPlaneInstanceStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, STATEMENTS, typeManager.getMapper(), queryExecutor, clock, CONNECTOR_NAME);
        helper.truncateTable(STATEMENTS.getDataPlaneInstanceTable());
    }


    @Override
    protected DataPlaneInstanceStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String entityId, String owner, Duration duration) {
        leaseUtil.leaseEntity(entityId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String entityId, String owner) {
        return leaseUtil.isLeased(entityId, owner);
    }

    @Override
    protected Duration getTestTimeout() {
        return Duration.ofSeconds(2);
    }
}
