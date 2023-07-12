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
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.store.sql.assetindex.SqlAssetIndex;
import org.eclipse.edc.connector.store.sql.assetindex.schema.BaseSqlDialectStatements;
import org.eclipse.edc.connector.store.sql.assetindex.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.testfixtures.asset.AssetIndexTestBase;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import javax.sql.DataSource;

import static org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions.createDataSource;

@ParallelPostgresCosmosTest
@ExtendWith(EdcExtension.class)
public class CosmosAssetIndexTest extends AssetIndexTestBase {
    private final BaseSqlDialectStatements sqlStatements = new PostgresDialectStatements();
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlAssetIndex sqlAssetIndex;
    private NoopTransactionContext transactionContext;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        var typeManager = new TypeManager();
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        dataSource = createDataSource();
        reg.register(dsName, dataSource);

        System.setProperty("edc.datasource.asset.name", dsName);

        transactionContext = new NoopTransactionContext();
        sqlAssetIndex = new SqlAssetIndex(reg, dsName, transactionContext, new ObjectMapper(), sqlStatements, queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("schema.sql");
        runQuery(schema);
    }

    @AfterEach
    void tearDown() {
        runQuery("DROP TABLE " + sqlStatements.getAssetTable() + " CASCADE");
        runQuery("DROP TABLE " + sqlStatements.getDataAddressTable() + " CASCADE");
        runQuery("DROP TABLE " + sqlStatements.getAssetPropertyTable() + " CASCADE");
    }

    @Override
    protected SqlAssetIndex getAssetIndex() {
        return sqlAssetIndex;
    }

    private void runQuery(String schema) {
        try (var connection = dataSource.getConnection()) {
            transactionContext.execute(() -> queryExecutor.execute(connection, schema));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
