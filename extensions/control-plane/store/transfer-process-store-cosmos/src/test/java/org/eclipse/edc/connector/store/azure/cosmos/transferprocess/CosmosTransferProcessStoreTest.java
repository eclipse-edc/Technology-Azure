/*
 *  Copyright (c) 2023 Bayerische Motorenwerke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motorenwerke Aktiengesellschaft - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.transferprocess;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.PostgresCosmosTest;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.controlplane.store.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store.TestFunctions;
import org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store.TransferProcessStoreTestBase;
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
import java.time.Duration;
import javax.sql.DataSource;

import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;

@PostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
class CosmosTransferProcessStoreTest extends TransferProcessStoreTestBase {

    private static final PostgresDialectStatements STATEMENTS = new PostgresDialectStatements();
    private SqlTransferProcessStore store;
    private LeaseUtil leaseUtil;

    @BeforeAll
    static void createDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getTransferProcessTableName());
        helper.dropTable(STATEMENTS.getLeaseTableName());
    }

    @BeforeEach
    void setUp(DataSourceRegistry reg, TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper, DataSource datasource) {

        var typeManager = new JacksonTypeManager();
        typeManager.registerTypes(TestFunctions.TestResourceDef.class, TestFunctions.TestProvisionedResource.class);
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));


        store = new SqlTransferProcessStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, typeManager.getMapper(), STATEMENTS, "test-connector", clock, queryExecutor);

        leaseUtil = new LeaseUtil(transactionContext, () -> {
            try {
                return datasource.getConnection();
            } catch (SQLException e) {
                throw new AssertionError(e);
            }
        }, STATEMENTS, clock);

        helper.truncateTable(STATEMENTS.getTransferProcessTableName());
        helper.truncateTable(STATEMENTS.getLeaseTableName());
    }

    @Override
    protected SqlTransferProcessStore getTransferProcessStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String negotiationId, String owner, Duration duration) {
        leaseUtil.leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String negotiationId, String owner) {
        return leaseUtil.isLeased(negotiationId, owner);
    }

}

