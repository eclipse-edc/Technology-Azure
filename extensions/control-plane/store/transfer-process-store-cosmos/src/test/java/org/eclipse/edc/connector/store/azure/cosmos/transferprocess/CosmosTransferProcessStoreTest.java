package org.eclipse.edc.connector.store.azure.cosmos.transferprocess;


import org.eclipse.edc.azure.testfixtures.CosmosPostgresFunctions;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.store.sql.transferprocess.store.SqlTransferProcessStore;
import org.eclipse.edc.connector.store.sql.transferprocess.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions;
import org.eclipse.edc.connector.transfer.spi.testfixtures.store.TransferProcessStoreTestBase;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResourceSet;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createDataRequest;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createTransferProcess;
import static org.eclipse.edc.connector.transfer.spi.testfixtures.store.TestFunctions.createTransferProcessBuilder;

@AzureCosmosDbIntegrationTest
@ExtendWith(EdcExtension.class)
class CosmosTransferProcessStoreTest extends TransferProcessStoreTestBase {

    private final Clock clock = Clock.systemUTC();
    private final PostgresDialectStatements statements = new PostgresDialectStatements();
    private final QueryExecutor queryExecutor = new SqlQueryExecutor();
    private SqlTransferProcessStore store;
    private LeaseUtil leaseUtil;
    private DataSource dataSource;
    private NoopTransactionContext transactionContext;

    @BeforeEach
    void setUp() {

        var typeManager = new TypeManager();
        typeManager.registerTypes(TestFunctions.TestResourceDef.class, TestFunctions.TestProvisionedResource.class);
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));


        dataSource = CosmosPostgresFunctions.createDataSource();
        var dsName = "test-ds";
        var reg = new DefaultDataSourceRegistry();
        reg.register(dsName, dataSource);

        System.setProperty("edc.datasource.contractnegotiation.name", dsName);

        transactionContext = new NoopTransactionContext();
        leaseUtil = new LeaseUtil(transactionContext, this::getConnection, statements, clock);
        store = new SqlTransferProcessStore(reg, dsName, transactionContext, typeManager.getMapper(), statements, "test-connector", clock, queryExecutor);

        var schema = TestUtils.getResourceFileContentAsString("schema.sql");
        runQuery(schema);
        leaseUtil = new LeaseUtil(transactionContext, this::getConnection, statements, clock);
    }

    @AfterEach
    void tearDown() {
        runQuery("DROP TABLE " + statements.getTransferProcessTableName() + " CASCADE");
        runQuery("DROP TABLE " + statements.getDataRequestTable() + " CASCADE");
        runQuery("DROP TABLE " + statements.getLeaseTableName() + " CASCADE");
    }

    @Test
    void find_queryByDataRequest_propNotExist() {
        var da = createDataRequest();
        var tp = createTransferProcessBuilder("testprocess1")
                .dataRequest(da)
                .build();
        store.updateOrCreate(tp);
        store.updateOrCreate(createTransferProcess("testprocess2"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("dataRequest.notexist", "=", "somevalue")))
                .build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed");
    }


    @Test
    void find_queryByResourceManifest_propNotExist() {
        var rm = ResourceManifest.Builder.newInstance()
                .definitions(List.of(TestFunctions.TestResourceDef.Builder.newInstance().id("rd-id").transferProcessId("testprocess1").build())).build();
        var tp = createTransferProcessBuilder("testprocess1")
                .resourceManifest(rm)
                .build();
        store.updateOrCreate(tp);
        store.updateOrCreate(createTransferProcess("testprocess2"));

        // throws exception when an explicit mapping exists
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.foobar", "=", "someval")))
                .build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

        // returns empty when the invalid value is embedded in JSON
        var query2 = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("resourceManifest.definitions.notexist", "=", "someval")))
                .build();

        assertThat(store.findAll(query2)).isEmpty();
    }


    @Test
    void find_queryByProvisionedResourceSet_propNotExist() {
        var resource = TestFunctions.TestProvisionedResource.Builder.newInstance()
                .resourceDefinitionId("rd-id")
                .transferProcessId("testprocess1")
                .id("pr-id")
                .build();
        var prs = ProvisionedResourceSet.Builder.newInstance()
                .resources(List.of(resource))
                .build();
        var tp = createTransferProcessBuilder("testprocess1")
                .provisionedResourceSet(prs)
                .build();
        store.updateOrCreate(tp);
        store.updateOrCreate(createTransferProcess("testprocess2"));

        // throws exception when an explicit mapping exists
        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.foobar.transferProcessId", "=", "testprocess1")))
                .build();
        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

        // returns empty when the invalid value is embedded in JSON
        var query2 = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("provisionedResourceSet.resources.foobar", "=", "someval")))
                .build();

        assertThat(store.findAll(query2)).isEmpty();
    }


    @Test
    void find_queryByLease() {
        store.updateOrCreate(createTransferProcess("testprocess1"));

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("lease.leasedBy", "=", "foobar")))
                .build();

        assertThatThrownBy(() -> store.findAll(query)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    @Test
    void create_withoutDataRequest_throwsException() {
        var t1 = TestFunctions.createTransferProcessBuilder("id1")
                .dataRequest(null)
                .build();
        assertThatIllegalArgumentException().isThrownBy(() -> getTransferProcessStore().updateOrCreate(t1));
    }

    @Override
    @Test
    protected void findAll_verifySorting_invalidProperty() {
        range(0, 10).forEach(i -> getTransferProcessStore().updateOrCreate(createTransferProcess("test-neg-" + i)));

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        assertThatThrownBy(() -> getTransferProcessStore().findAll(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Override
    protected boolean supportsCollectionQuery() {
        return true;
    }

    @Override
    protected boolean supportsLikeOperator() {
        return true;
    }

    @Override
    protected SqlTransferProcessStore getTransferProcessStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        getLeaseUtil().leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return getLeaseUtil().isLeased(negotiationId, owner);
    }

    protected LeaseUtil getLeaseUtil() {
        return leaseUtil;
    }

    private void runQuery(String schema) {
        try (var connection = dataSource.getConnection()) {
            transactionContext.execute(() -> queryExecutor.execute(connection, schema));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

