/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.assetindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.store.sql.assetindex.SqlAssetIndex;
import org.eclipse.edc.connector.store.sql.assetindex.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.store.sql.assetindex.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.testfixtures.asset.AssetIndexTestBase;
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
public class CosmosAssetIndexTest extends AssetIndexTestBase {
    private static final BaseSqlDialectStatements SQL_STATEMENTS = new PostgresDialectStatements();
    private SqlAssetIndex sqlAssetIndex;

    @BeforeEach
    void setUp(TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper runner, DataSourceRegistry reg) {
        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));


        sqlAssetIndex = new SqlAssetIndex(reg, DEFAULT_DATASOURCE_NAME, transactionContext, new ObjectMapper(), SQL_STATEMENTS, queryExecutor);

        runner.truncateTable(SQL_STATEMENTS.getAssetTable());
        runner.truncateTable(SQL_STATEMENTS.getDataAddressTable());
        runner.truncateTable(SQL_STATEMENTS.getAssetPropertyTable());
    }

    @Override
    protected SqlAssetIndex getAssetIndex() {
        return sqlAssetIndex;
    }

    @BeforeAll
    static void prepare(CosmosPostgresTestExtension.SqlHelper runner) {
        runner.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper runner) {
        runner.dropTable(SQL_STATEMENTS.getAssetTable());
        runner.dropTable(SQL_STATEMENTS.getDataAddressTable());
        runner.dropTable(SQL_STATEMENTS.getAssetPropertyTable());
    }


}
