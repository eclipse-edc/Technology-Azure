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

package org.eclipse.edc.azure.testfixtures;

import org.postgresql.ds.PGSimpleDataSource;

import java.util.Objects;
import javax.sql.DataSource;

import static org.eclipse.edc.util.configuration.ConfigurationFunctions.propOrEnv;

public class CosmosPostgresFunctions {
    private static final String PG_CONNECTION_STRING = "PG_CONNECTION_STRING";

    public static DataSource createDataSource() {


        var connectionString = propOrEnv(PG_CONNECTION_STRING, null);
        Objects.requireNonNull(connectionString, "CosmosDB Postgres connection string not found");

        var ds = new PGSimpleDataSource();
        ds.setURL(connectionString);
        return ds;
    }
}
