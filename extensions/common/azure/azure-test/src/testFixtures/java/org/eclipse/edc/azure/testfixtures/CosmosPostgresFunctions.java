package org.eclipse.edc.azure.testfixtures;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Objects;

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
