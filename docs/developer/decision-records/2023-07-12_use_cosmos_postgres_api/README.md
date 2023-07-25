# Use the PostgreSQL API for CosmosDB

## Decision

EDC will use the PostgreSQL API for CosmosDB instead of Cosmos' own client SDK ("SQL API").

## Rationale

Now that Cosmos fully supports PostgreSQL (https://learn.microsoft.com/en-us/azure/cosmos-db/postgresql/introduction), there is no need to keep the CosmosDB client around. CosmosDB can now be targeted using the JDBC PostgreSQL driver. 
This will greatly reduce the maintenance surface, and it may even reduce test runtime.

These store implementations do not yet have a PostgreSQL variant:
- `FederatedCacheNodeDirectory`


## Approach

- delete all specific CosmosDB implementations for persistence, such as `AssetIndex`, etc.
- delete the `CosmosFederatedCacheNodeDirectory`, as it is not used currently
- for our integration tests, we'll need to provision a "Cosmos Postgres cluster" and retire our existing CosmosDB
- for each store implementation, add tests in the `Technology-Azure` repo targeting a CosmosDB PostgreSQL cluster specifically, for example:
  ```java
  @ComponentTest
  @ExtendWith(EdcExtension.class)
  public class CosmosPostgresTest extends AssetIndexTestBase {
      private final BaseSqlDialectStatements sqlStatements = new PostgresDialectStatements();
      private final QueryExecutor queryExecutor = new SqlQueryExecutor();
      private SqlAssetIndex sqlAssetIndex;
      private NoopTransactionContext transactionContext;
      private DataSource dataSource;

      @BeforeEach
      void setUp()  {
          var typeManager = new TypeManager();
          typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

          var dsName = "test-ds";
          var reg = new DefaultDataSourceRegistry();
          dataSource = createDataSource();
          reg.register(dsName, dataSource);

          System.setProperty("edc.datasource.asset.name", dsName);

          transactionContext = new NoopTransactionContext();
          sqlAssetIndex = new SqlAssetIndex(reg, dsName, transactionContext, new ObjectMapper(), sqlStatements, queryExecutor);

          var schema = Files.readString(Paths.get("docs/schema.sql"));
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

      private DataSource createDataSource() {
          var ds = new PGSimpleDataSource();
          // this can be obtained after setting up the Cosmos-PG cluster
          // should be injected through environment variables
          ds.setServerNames(new String[]{ "c-edc-pg-test-cluster.pnrboctaun4gkt.postgres.cosmos.azure.com" });
          ds.setPortNumbers(new int[]{ 5432 });
          ds.setUser("<USER>");
          ds.setPassword("<PASSWORD>");
          ds.setDatabaseName("<DBNAME>");
          ds.setSslmode("require");
          return ds;
      }

      private void runQuery(String schema) {
          try (var connection = dataSource.getConnection()) {
              transactionContext.execute(() -> queryExecutor.execute(connection, schema));
          } catch (SQLException e) {
              throw new RuntimeException(e);
          }
      }
  }
  ```