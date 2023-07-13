/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation;

import org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension;
import org.eclipse.edc.azure.testfixtures.annotations.PostgresCosmosTest;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.ContractNegotiationStoreTestBase;
import org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.SqlContractNegotiationStore;
import org.eclipse.edc.connector.store.sql.contractnegotiation.store.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.testfixtures.LeaseUtil;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.azure.testfixtures.CosmosPostgresTestExtension.DEFAULT_DATASOURCE_NAME;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createContract;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createContractBuilder;
import static org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.TestFunctions.createNegotiation;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getResourceFileContentAsString;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * This test aims to verify those parts of the contract negotiation store, that are specific to Postgres, e.g. JSON
 * query operators.
 */
@PostgresCosmosTest
@ExtendWith(CosmosPostgresTestExtension.class)
class CosmosContractNegotiationStoreTest extends ContractNegotiationStoreTestBase {
    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final PostgresDialectStatements STATEMENTS = new PostgresDialectStatements();
    private final Clock clock = Clock.systemUTC();
    private SqlContractNegotiationStore store;
    private LeaseUtil leaseUtil;

    @AfterAll
    static void dropTables(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.dropTable(STATEMENTS.getContractNegotiationTable());
        helper.dropTable(STATEMENTS.getContractAgreementTable());
        helper.dropTable(STATEMENTS.getLeaseTableName());
    }

    @BeforeAll
    static void setupDatabase(CosmosPostgresTestExtension.SqlHelper helper) {
        helper.executeStatement(getResourceFileContentAsString("schema.sql"));
    }

    @BeforeEach
    void setUp(DataSource dataSource, TransactionContext transactionContext, QueryExecutor queryExecutor, CosmosPostgresTestExtension.SqlHelper helper, DataSourceRegistry reg) {
        var statements = new PostgresDialectStatements();
        var manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));

        store = new SqlContractNegotiationStore(reg, DEFAULT_DATASOURCE_NAME, transactionContext, manager.getMapper(), statements, CONNECTOR_NAME, clock, queryExecutor);
        leaseUtil = new LeaseUtil(transactionContext, () -> {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new AssertionError(e);
            }
        }, statements, clock);

        helper.truncateTable(STATEMENTS.getContractNegotiationTable());
        helper.truncateTable(STATEMENTS.getContractAgreementTable());
        helper.truncateTable(STATEMENTS.getLeaseTableName());
    }

    @Test
    void query_byAgreementId() {
        var contractId1 = ContractId.create("def1", "asset");
        var contractId2 = ContractId.create("def2", "asset");
        var negotiation1 = createNegotiation("neg1", createContract(contractId1));
        var negotiation2 = createNegotiation("neg2", createContract(contractId2));
        store.save(negotiation1);
        store.save(negotiation2);

        var expression = criterion("contractAgreement.id", "=", contractId1.toString());
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation1);
    }

    @Test
    void query_byPolicyAssignee() {

        var policy = Policy.Builder.newInstance()
                .assignee("test-assignee")
                .assigner("test-assigner")
                .permission(Permission.Builder.newInstance()
                        .target("")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .constraint(AtomicConstraint.Builder.newInstance()
                                .leftExpression(new LiteralExpression("foo"))
                                .operator(Operator.EQ)
                                .rightExpression(new LiteralExpression("bar"))
                                .build())
                        .build())
                .build();

        var agreement1 = createContractBuilder("agr1").policy(policy).build();
        var agreement2 = createContractBuilder("agr2").policy(policy).build();
        var negotiation1 = createNegotiation("neg1", agreement1);
        var negotiation2 = createNegotiation("neg2", agreement2);
        store.save(negotiation1);
        store.save(negotiation2);

        var expression = criterion("contractAgreement.policy.assignee", "=", "test-assignee");
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(negotiation1, negotiation2);
    }

    @Test
    void query_invalidKey_shouldThrowException() {
        var contractId = ContractId.create("definition", "asset");
        var agreement1 = createContract(contractId);
        var negotiation1 = createNegotiation("neg1", agreement1);
        store.save(negotiation1);

        var expression = criterion("contractAgreement.notexist", "=", contractId.toString());
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        assertThatThrownBy(() -> store.queryNegotiations(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Test
    void query_invalidKeyInJson() {
        var contractId = ContractId.create("definition", "asset");
        var agreement1 = createContract(contractId);
        var negotiation1 = createNegotiation("neg1", agreement1);
        store.save(negotiation1);

        var expression = criterion("contractAgreement.policy.permissions.notexist", "=", "foobar");
        var query = QuerySpec.Builder.newInstance().filter(expression).build();
        assertThat(store.queryNegotiations(query)).isEmpty();
    }

    @Test
    void queryAgreements_withQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.create(UUID.randomUUID().toString(), TEST_ASSET_ID).toString())
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter(criterion("assetId", "=", "asset-2")).build();
        var all = store.queryAgreements(query);

        assertThat(all).hasSize(1);
    }

    @Test
    void queryAgreements_withQuerySpec_invalidOperand() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.create(UUID.randomUUID().toString(), TEST_ASSET_ID).toString())
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter(criterion("notexistprop", "=", "asset-2")).build();
        assertThatThrownBy(() -> store.queryAgreements(query));
    }

    @Test
    void queryAgreements_withQuerySpec_noFilter() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.create(UUID.randomUUID().toString(), TEST_ASSET_ID).toString())
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().offset(2).limit(2).build();
        assertThat(store.queryAgreements(query)).hasSize(2);
    }

    @Test
    void queryAgreements_withQuerySpec_invalidValue() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder(ContractId.create(UUID.randomUUID().toString(), TEST_ASSET_ID).toString())
                    .assetId("asset-" + i)
                    .build();
            var negotiation = createNegotiation(UUID.randomUUID().toString(), contractAgreement);
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().filter(criterion("assetId", "=", "notexist")).build();
        assertThat(store.queryAgreements(query)).isEmpty();
    }

    @Test
    void create_and_cancel_contractAgreement() {
        var negotiationId = "test-cn1";
        var negotiation = createNegotiation(negotiationId);
        store.save(negotiation);

        // now add the agreement
        var agreement = createContract(ContractId.create("definition", "asset"));
        var updatedNegotiation = createNegotiation(negotiationId, agreement);

        store.save(updatedNegotiation);
        assertThat(store.queryAgreements(QuerySpec.none()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(agreement);

        // cancel the agreement
        updatedNegotiation.transitionTerminating("Cancelled");
        store.save(updatedNegotiation);
    }

    @Test
    void nextNotLeased_typeFilter() {
        range(0, 5).mapToObj(it -> TestFunctions.createNegotiationBuilder("1" + it)
                .state(REQUESTED.code())
                .type(PROVIDER)
                .build()).forEach(store::save);
        range(5, 10).mapToObj(it -> TestFunctions.createNegotiationBuilder("1" + it)
                .state(REQUESTED.code())
                .type(CONSUMER)
                .build()).forEach(store::save);
        var criteria = new Criterion[] {hasState(REQUESTED.code()), new Criterion("type", "=", "CONSUMER")};

        var result = store.nextNotLeased(10, criteria);

        assertThat(result).hasSize(5).allMatch(it -> it.getType() == CONSUMER);
    }

    @Override
    protected SqlContractNegotiationStore getContractNegotiationStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        leaseUtil.leaseEntity(negotiationId, owner, duration);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        return leaseUtil.isLeased(negotiationId, owner);
    }
}
