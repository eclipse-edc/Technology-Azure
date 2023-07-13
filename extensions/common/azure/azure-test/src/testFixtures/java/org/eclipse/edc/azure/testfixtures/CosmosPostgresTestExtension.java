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

package org.eclipse.edc.azure.testfixtures;

import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import static org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions.createDataSource;

public class CosmosPostgresTestExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    public static final String DEFAULT_DATASOURCE_NAME = "test-datasource";
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private DataSource dataSource;
    private DataSourceRegistry registry;

    @Override
    public void beforeAll(ExtensionContext context) {
        dataSource = createDataSource();

    }


    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        return List.of(CosmosPostgresTestExtension.class, QueryExecutor.class, DataSource.class, DataSourceRegistry.class, TransactionContext.class, SqlHelper.class).contains(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(CosmosPostgresTestExtension.class)) {
            return this;
        } else if (type.equals(QueryExecutor.class)) {
            return queryExecutor;
        } else if (type.equals(TransactionContext.class)) {
            return transactionContext;
        } else if (type.equals(DataSource.class)) {
            return dataSource;
        } else if (type.equals(SqlHelper.class)) {
            return new SqlHelper();
        } else if (type.equals(DataSourceRegistry.class)) {
            return registry;
        }
        return null;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        registry = new DefaultDataSourceRegistry();
        registry.register(DEFAULT_DATASOURCE_NAME, dataSource);
    }

    /**
     * This class provides helper methods for executing SQL statements and handling database operations.
     */
    public class SqlHelper {

        /**
         * Executes a database statement with the given SQL statement.
         *
         * @param schema the SQL statement
         */
        public void executeStatement(String schema) {
            try (var connection = dataSource.getConnection()) {
                transactionContext.execute(() -> queryExecutor.execute(connection, schema));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Drops a table from the database with the given table name.
         *
         * @param tableName the name of the table to be dropped
         */
        public void dropTable(String tableName) {
            executeStatement("DROP TABLE " + tableName + " CASCADE");
        }

        /**
         * Truncates (empties) a table in the database with the given table name.
         *
         * @param tableName the name of the table to be truncated
         */
        public void truncateTable(String tableName) {
            executeStatement("TRUNCATE TABLE " + tableName + " CASCADE");
        }
    }
}
