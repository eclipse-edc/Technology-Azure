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

package org.eclipse.edc.connector.dataplane.store.cosmos;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.ParallelPostgresCosmosTest;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.SqlDataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.sql.schema.postgres.PostgresDataPlaneStatements;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.entity.MutableEntity;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.RECEIVED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_LEASED;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.hamcrest.Matchers.hasSize;

/**
 * This test DOES NOT inherit {@link org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase} because there, time limits
 * are set hard coded to 500 ms, which is too short for Postgres@CosmosDB. Unfortunately there also is no way to override a nested test class.
 */
@ParallelPostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
public class CosmosDataPlaneStoreTest /* extends DataPlaneStoreTestBase */ {
    private static final String CONNECTOR_NAME = "test-connector";
    private static final int TIMEOUT = 2000;
    private static final PostgresDataPlaneStatements STATEMENTS = new PostgresDataPlaneStatements();
    private final Clock clock = Clock.systemUTC();
    private SqlDataPlaneStore store;
    private LeaseUtil leaseUtil;

    @BeforeAll
    static void setupDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getDataPlaneTable());
    }

    @BeforeEach
    void setUp(TransactionContext transactionContext, QueryExecutor queryExecutor, DataSourceRegistry reg, CosmosPostgresTestExtension.SqlHelper helper) {
        var typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstance.class);
        typeManager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        leaseUtil = new LeaseUtil(transactionContext, helper.connectionSupplier(), STATEMENTS, clock);

        store = new SqlDataPlaneStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, STATEMENTS, typeManager.getMapper(), clock, queryExecutor, "test-connector");
        helper.truncateTable(STATEMENTS.getDataPlaneTable());
    }

    protected DataPlaneStore getStore() {
        return store;
    }

    protected void leaseEntity(String entityId, String owner) {
        leaseEntity(entityId, owner, Duration.ofSeconds(60));
    }

    protected void leaseEntity(String negotiationId, String owner, Duration duration) {
        getLeaseUtil().leaseEntity(negotiationId, owner, duration);
    }

    protected boolean isLeasedBy(String negotiationId, String owner) {
        return getLeaseUtil().isLeased(negotiationId, owner);
    }

    protected LeaseUtil getLeaseUtil() {
        return leaseUtil;
    }

    private DataFlow createDataFlow(String id, DataFlowStates state) {
        return DataFlow.Builder.newInstance()
                .id(id)
                .callbackAddress(URI.create("http://any"))
                .source(DataAddress.Builder.newInstance().type("src-type").build())
                .destination(DataAddress.Builder.newInstance().type("dest-type").build())
                .trackable(true)
                .state(state.code())
                .build();
    }

    @Nested
    class Create {

        @Test
        void shouldStoreEntity_whenItDoesNotAlreadyExist() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            var result = getStore().findById(dataFlow.getId());

            assertThat(result).isNotNull().usingRecursiveComparison().isEqualTo(dataFlow);
            assertThat(result.getCreatedAt()).isGreaterThan(0);
        }

        @Test
        void shouldUpdate_whenEntityAlreadyExist() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            dataFlow.transitToCompleted();
            getStore().save(dataFlow);

            var result = getStore().findById(dataFlow.getId());

            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(COMPLETED.code());
        }
    }

    @Nested
    class NextNotLeased {
        @Test
        void shouldReturnNotLeasedItems() {
            var state = RECEIVED;
            var all = range(0, 5)
                    .mapToObj(i -> createDataFlow("id-" + i, state))
                    .peek(getStore()::save)
                    .peek(this::delayByTenMillis)
                    .toList();

            var leased = getStore().nextNotLeased(2, hasState(state.code()));

            assertThat(leased).hasSize(2).extracting(DataFlow::getId)
                    .isSubsetOf(all.stream().map(Entity::getId).toList())
                    .allMatch(id -> isLeasedBy(id, CONNECTOR_NAME));

            assertThat(leased).extracting(MutableEntity::getUpdatedAt).isSorted();
        }

        @Test
        void shouldReturnFreeEntities() {
            var state = RECEIVED;
            var all = range(0, 5)
                    .mapToObj(i -> createDataFlow("id-" + i, state))
                    .peek(getStore()::save)
                    .toList();

            var firstLeased = getStore().nextNotLeased(2, hasState(state.code()));
            var leased = getStore().nextNotLeased(2, hasState(state.code()));

            assertThat(leased.stream().map(Entity::getId)).hasSize(2)
                    .isSubsetOf(all.stream().map(Entity::getId).toList())
                    .doesNotContainAnyElementsOf(firstLeased.stream().map(Entity::getId).toList());
        }

        @Test
        void shouldReturnFreeItemInTheExpectedState() {
            range(0, 5)
                    .mapToObj(i -> createDataFlow("id-" + i, RECEIVED))
                    .forEach(getStore()::save);

            var leased = getStore().nextNotLeased(2, hasState(COMPLETED.code()));

            assertThat(leased).isEmpty();
        }

        @Test
        void shouldLeaseAgainAfterTimePassed() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            leaseEntity(dataFlow.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

            await().atMost(Duration.ofMillis(TIMEOUT))
                    .until(() -> getStore().nextNotLeased(1, hasState(RECEIVED.code())), hasSize(1));
        }

        @Test
        void shouldReturnReleasedEntityByUpdate() {
            var dataFlow = createDataFlow(UUID.randomUUID().toString(), RECEIVED);
            getStore().save(dataFlow);

            var firstLeased = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(firstLeased).hasSize(1);

            var secondLeased = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(secondLeased).isEmpty();

            getStore().save(firstLeased.get(0));

            var thirdLeased = getStore().nextNotLeased(1, hasState(RECEIVED.code()));
            assertThat(thirdLeased).hasSize(1);
        }

        private void delayByTenMillis(StatefulEntity<?> t) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                // noop
            }
            t.updateStateTimestamp();
        }
    }

    @Nested
    class FindByIdAndLease {
        @Test
        void shouldReturnTheEntityAndLeaseIt() {
            var id = UUID.randomUUID().toString();
            getStore().save(createDataFlow(id, RECEIVED));

            var result = getStore().findByIdAndLease(id);

            AbstractResultAssert.assertThat(result).isSucceeded();
            assertThat(isLeasedBy(id, CONNECTOR_NAME)).isTrue();
        }

        @Test
        void shouldReturnNotFound_whenEntityDoesNotExist() {
            var result = getStore().findByIdAndLease("unexistent");

            AbstractResultAssert.assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldReturnAlreadyLeased_whenEntityIsAlreadyLeased() {
            var id = UUID.randomUUID().toString();
            getStore().save(createDataFlow(id, RECEIVED));
            leaseEntity(id, "other owner");

            var result = getStore().findByIdAndLease(id);

            AbstractResultAssert.assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_LEASED);
        }
    }
}
