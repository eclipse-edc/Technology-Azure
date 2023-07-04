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
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.BadRequestException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosStoredProcedureProperties;
import com.azure.cosmos.models.PartitionKey;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.testfixtures.negotiation.store.ContractNegotiationStoreTestBase;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.model.ContractNegotiationDocument;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyRegistrationTypes;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions.createContractBuilder;
import static org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions.createNegotiation;
import static org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions.createNegotiationBuilder;
import static org.eclipse.edc.connector.store.azure.cosmos.contractnegotiation.TestFunctions.generateDocument;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.query.Criterion.criterion;

@AzureCosmosDbIntegrationTest
class CosmosContractNegotiationStoreIntegrationTest extends ContractNegotiationStoreTestBase {
    public static final String CONNECTOR_ID = "test-connector";
    private static final String TEST_ID = UUID.randomUUID().toString();
    private static final String DATABASE_NAME = "connector-itest-" + TEST_ID;
    private static final String CONTAINER_PREFIX = "ContractNegotiationStore-";
    private static CosmosContainer container;
    private static CosmosDatabase database;
    private final Clock clock = Clock.systemUTC();
    private final String partitionKey = CONNECTOR_ID;
    private TypeManager typeManager;
    private CosmosContractNegotiationStore store;

    @BeforeAll
    static void prepareCosmosClient() {
        var client = CosmosTestClient.createClient();

        var response = client.createDatabaseIfNotExists(DATABASE_NAME);
        database = client.getDatabase(response.getProperties().getId());
    }

    @AfterAll
    static void deleteDatabase() {
        if (database != null) {
            var delete = database.delete();
            assertThat(delete.getStatusCode()).isBetween(200, 300);
        }
    }

    @BeforeEach
    void setUp() {
        var containerName = CONTAINER_PREFIX + UUID.randomUUID();
        var containerIfNotExists = database.createContainerIfNotExists(containerName, "/partitionKey");
        container = database.getContainer(containerIfNotExists.getProperties().getId());
        uploadStoredProcedure(container, "nextForState");
        uploadStoredProcedure(container, "lease");
        assertThat(database).describedAs("CosmosDB database is null - did something go wrong during initialization?").isNotNull();

        typeManager = new TypeManager();
        typeManager.registerTypes(ContractDefinition.class, ContractNegotiationDocument.class);
        PolicyRegistrationTypes.TYPES.forEach(typeManager::registerTypes);
        var cosmosDbApi = new CosmosDbApiImpl(container, true);
        var retryPolicy = RetryPolicy.builder().withMaxRetries(3).withBackoff(1, 5, ChronoUnit.SECONDS).build();
        store = new CosmosContractNegotiationStore(cosmosDbApi, typeManager, retryPolicy, CONNECTOR_ID, clock);
    }

    @AfterEach
    void tearDown() {
        container.delete();
    }

    @Test
    void findById() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.findById(doc1.getId());

        assertThat(foundItem).isNotNull().usingRecursiveComparison().isEqualTo(doc1.getWrappedInstance());
    }

    @Test
    void findById_notExist() {
        var foundItem = store.findById("not-exit");
        assertThat(foundItem).isNull();
    }

    @Test
    void findForCorrelationId() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var corrId = doc1.getWrappedInstance().getCorrelationId();
        var foundItem = store.findForCorrelationId(corrId);

        assertThat(foundItem).isNotNull().usingRecursiveComparison().isEqualTo(doc1.getWrappedInstance());
    }

    @Test
    void findForCorrelationId_notFound() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.findForCorrelationId("not-exit");

        assertThat(foundItem).isNull();
    }

    @Test
    void findContractAgreement() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        var foundItem = store.findContractAgreement(doc1.getWrappedInstance().getContractAgreement().getId());

        assertThat(foundItem).isNotNull().usingRecursiveComparison().isEqualTo(doc1.getWrappedInstance().getContractAgreement());
    }

    @Test
    void findContractAgreement_notFound() {
        var foundItem = store.findContractAgreement("not-exist");

        assertThat(foundItem).isNull();
    }

    @Test
    void save_notExists_shouldCreate() {
        var negotiation = TestFunctions.createNegotiation();
        store.save(negotiation);

        var allObjs = container.readAllItems(new PartitionKey(partitionKey), Object.class);

        assertThat(allObjs).hasSize(1).allSatisfy(o -> assertThat(toNegotiation(o)).usingRecursiveComparison().isEqualTo(negotiation));
    }

    @Test
    void save_exists_shouldUpdate() {
        var negotiation = TestFunctions.createNegotiation();
        container.createItem(new ContractNegotiationDocument(negotiation, partitionKey));

        assertThat(container.readAllItems(new PartitionKey(partitionKey), Object.class)).hasSize(1);

        //add an offer, should modify
        var newOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .assetId("new-offer-1")
                .build();
        negotiation.getContractOffers().add(newOffer);
        store.save(negotiation);

        var allObjs = container.readAllItems(new PartitionKey(partitionKey), Object.class);

        assertThat(allObjs).hasSize(1).allSatisfy(o -> {
            var actual = toNegotiation(o);
            assertThat(actual.getContractOffers()).hasSize(1).extracting(ContractOffer::getId).containsExactlyInAnyOrder(newOffer.getId());
        });
    }

    @Test
    void save_leasedByOther_shouldRaiseException() {
        var negotiation = createNegotiation("test-id", ContractNegotiationStates.AGREED);
        var item = new ContractNegotiationDocument(negotiation, partitionKey);
        item.acquireLease("someone-else", clock);
        container.createItem(item);

        negotiation.transitionTerminating("test-error");

        assertThatThrownBy(() -> store.save(negotiation)).isInstanceOf(IllegalStateException.class).hasRootCauseInstanceOf(BadRequestException.class);
    }

    @Test
    void delete_leasedByOther_shouldRaiseException() {
        var negotiation = createNegotiation("test-id", ContractNegotiationStates.AGREED);
        var item = new ContractNegotiationDocument(negotiation, partitionKey);
        item.acquireLease("someone-else", clock);
        container.createItem(item);

        assertThatThrownBy(() -> store.delete(negotiation.getId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nextNotLeased() {
        var state = ContractNegotiationStates.AGREED;
        var n = TestFunctions.createNegotiation(state);
        container.createItem(new ContractNegotiationDocument(n, partitionKey));

        var result = store.nextNotLeased(10, hasState(state.code()));
        assertThat(result).hasSize(1).allSatisfy(neg -> assertThat(neg).usingRecursiveComparison().isEqualTo(n));
    }

    @Test
    void nextNotLeased_exceedsLimit() {
        var state = ContractNegotiationStates.AGREED;
        var numElements = 10;

        var preparedNegotiations = IntStream.range(0, numElements)
                .mapToObj(i -> TestFunctions.createNegotiation(state))
                .peek(n -> container.createItem(new ContractNegotiationDocument(n, partitionKey)))
                .collect(Collectors.toList());

        var result = store.nextNotLeased(4, hasState(state.code()));
        assertThat(result).hasSize(4).allSatisfy(r -> assertThat(preparedNegotiations).extracting(ContractNegotiation::getId).contains(r.getId()));
    }

    @Test
    void nextNotLeased_noResult() {
        var state = ContractNegotiationStates.AGREED;
        var n = TestFunctions.createNegotiation(state);
        container.createItem(new ContractNegotiationDocument(n, partitionKey));

        var result = store.nextNotLeased(10, hasState(OFFERING.code()));
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void nextNotLeased_onlyReturnsFreeItems() {
        var state = ContractNegotiationStates.AGREED;
        var n1 = TestFunctions.createNegotiation(state);
        var doc1 = new ContractNegotiationDocument(n1, partitionKey);
        container.createItem(doc1);

        var n2 = TestFunctions.createNegotiation(state);
        var doc2 = new ContractNegotiationDocument(n2, partitionKey);
        container.createItem(doc2);

        var n3 = TestFunctions.createNegotiation(state);
        var doc3 = new ContractNegotiationDocument(n3, partitionKey);
        doc3.acquireLease("another-connector", clock);
        container.createItem(doc3);

        var result = store.nextNotLeased(10, hasState(state.code()));
        assertThat(result).hasSize(2).extracting(ContractNegotiation::getId).containsExactlyInAnyOrder(n1.getId(), n2.getId());
    }

    @Test
    void nextNotLeased_leasedBySelf() {
        var state = ContractNegotiationStates.AGREED;
        var n = TestFunctions.createNegotiation(state);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        container.createItem(doc);

        // let's verify that the first invocation correctly sets the lease
        var result = store.nextNotLeased(10, hasState(state.code()));
        assertThat(result).hasSize(1); //should contain the lease already
        var storedNegotiation = readItem(n.getId());
        assertThat(storedNegotiation.getLease()).isNotNull().hasFieldOrPropertyWithValue("leasedBy", CONNECTOR_ID);

        // verify that the subsequent call to nextNotLeased does not return the entity
        result = store.nextNotLeased(10, hasState(state.code()));
        assertThat(result).isEmpty();
    }

    @Test
    void nextNotLeased_leasedByAnotherExpired() {
        var state = ContractNegotiationStates.AGREED;
        var n = TestFunctions.createNegotiation(state);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        Duration leaseDuration = Duration.ofSeconds(10); // give it some time to compensate for TOF delays
        doc.acquireLease("another-connector", clock, leaseDuration);
        container.createItem(doc);

        // before the lease expired
        var negotiationsBeforeLeaseExpired = store.nextNotLeased(10, hasState(state.code()));
        assertThat(negotiationsBeforeLeaseExpired).isEmpty();
        // after the lease expired
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .pollDelay(leaseDuration) //give the lease time to expire
                .untilAsserted(() -> {
                    List<ContractNegotiation> negotiationsAfterLeaseExpired = store.nextNotLeased(10, hasState(state.code()));
                    assertThat(negotiationsAfterLeaseExpired).hasSize(1).allSatisfy(neg -> assertThat(neg).usingRecursiveComparison().isEqualTo(n));
                });
    }

    @Test
    void nextNotLeased_verifySaveClearsLease() {
        var n = createNegotiation("test-id", REQUESTED);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        container.createItem(doc);

        var result = store.nextNotLeased(5, hasState(REQUESTED.code()));
        assertThat(result).hasSize(1).extracting(ContractNegotiation::getId).containsExactly(n.getId());
        var storedDoc = readItem(n.getId());
        assertThat(storedDoc.getLease()).isNotNull();
        assertThat(storedDoc.getLease().getLeasedBy()).isEqualTo(CONNECTOR_ID);
        assertThat(storedDoc.getLease().getLeasedAt()).isGreaterThan(0);
        assertThat(storedDoc.getLease().getLeaseDuration()).isEqualTo(60000L);

        n.transitionTerminating();
        n.updateStateTimestamp();
        store.save(n);

        storedDoc = readItem(n.getId());
        assertThat(storedDoc.getLease()).isNull();
        assertThat(container.readAllItems(new PartitionKey(partitionKey), Object.class)).hasSize(1);

    }

    @Test
    @DisplayName("Verify that a leased entity can not be deleted")
    void nextNotLeased_verifyDelete() {
        var n = createNegotiation("test-id", REQUESTED);
        var doc = new ContractNegotiationDocument(n, partitionKey);
        container.createItem(doc);

        var result = store.nextNotLeased(5, hasState(REQUESTED.code()));
        assertThat(result).hasSize(1).extracting(ContractNegotiation::getId).containsExactly(n.getId());

        // verify entity can be deleted
        assertThatThrownBy(() -> store.delete(n.getId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findAll_noQuerySpec() {
        var doc1 = generateDocument();
        var doc2 = generateDocument();

        container.createItem(doc1);
        container.createItem(doc2);

        assertThat(store.queryNegotiations(QuerySpec.none())).hasSize(2).extracting(ContractNegotiation::getId).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());
    }

    @Test
    void findAll_verifyPaging() {

        var all = IntStream.range(0, 10)
                .mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d))
                .map(ContractNegotiationDocument::getId)
                .collect(Collectors.toList());

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4).extracting(ContractNegotiation::getId).isSubsetOf(all);

    }

    @Test
    void findAll_verifyPaging_pageSizeLargerThanCollection() {

        var all = IntStream.range(0, 10)
                .mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d)).map(ContractNegotiationDocument::getId)
                .collect(Collectors.toList());

        // page size fits
        assertThat(store.queryNegotiations(QuerySpec.Builder.newInstance().offset(3).limit(40).build())).hasSize(7).extracting(ContractNegotiation::getId).isSubsetOf(all);
    }

    @Test
    void findAll_verifyFiltering() {
        var documents = IntStream.range(0, 10)
                .mapToObj(i -> generateDocument())
                .peek(d -> container.createItem(d))
                .collect(Collectors.toList());

        var expectedId = documents.get(3).getId();
        var query = QuerySpec.Builder.newInstance().filter(criterion("id", "=", expectedId)).build();

        assertThat(store.queryNegotiations(query)).extracting(ContractNegotiation::getId).containsOnly(expectedId);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument()).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter(criterion("something", "contains", "other")).build();

        assertThatThrownBy(() -> store.queryNegotiations(query)).isInstanceOfAny(IllegalArgumentException.class).hasMessage("Cannot build WHERE clause, reason: unsupported operator contains");
    }

    @Test
    void findAll_verifyFiltering_unsuccessfulFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument()).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().filter(criterion("something", "=", "other")).build();

        assertThat(store.queryNegotiations(query)).isEmpty();
    }

    @Test
    void findAll_verifySorting() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument()).forEach(d -> container.createItem(d));

        var ascendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.queryNegotiations(ascendingQuery)).hasSize(10).isSortedAccordingTo(Comparator.comparing(ContractNegotiation::getId));
        var descendingQuery = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.queryNegotiations(descendingQuery)).hasSize(10).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void findAll_verifySortingInvalidSortField() {
        IntStream.range(0, 10).mapToObj(i -> generateDocument()).forEach(d -> container.createItem(d));

        var query = QuerySpec.Builder.newInstance().sortField("xyz").sortOrder(SortOrder.ASC).build();
        var negotiations = store.queryNegotiations(query);

        assertThat(negotiations).hasSize(10);
    }

    @Test
    void queryAgreements_noQuerySpec() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder().id(ContractId.create(UUID.randomUUID().toString(), "test-asset-id").toString()).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        var all = store.queryAgreements(QuerySpec.Builder.newInstance().build());

        assertThat(all).hasSize(10);
    }

    @Test
    void queryAgreements_verifyPaging() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder().id(ContractId.create(UUID.randomUUID().toString(), "test-asset-id").toString()).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        // page size fits
        assertThat(store.queryAgreements(QuerySpec.Builder.newInstance().offset(3).limit(4).build())).hasSize(4);

        // page size too large
        assertThat(store.queryAgreements(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
    }

    @Test
    void queryAgreements_verifyFiltering() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder().id(i + ":" + i).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });
        var query = QuerySpec.Builder.newInstance().filter(criterion("id", "=", "3:3")).build();

        var result = store.queryAgreements(query);

        assertThat(result).extracting(ContractAgreement::getId).containsOnly("3:3");
    }

    @Test
    void queryAgreements_verifySorting() {
        IntStream.range(0, 9).forEach(i -> {
            var contractAgreement = createContractBuilder().id(i + ":" + i).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        var queryAsc = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(store.queryAgreements(queryAsc)).hasSize(9).isSortedAccordingTo(Comparator.comparing(ContractAgreement::getId));
        var queryDesc = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(store.queryAgreements(queryDesc)).hasSize(9).isSortedAccordingTo((c1, c2) -> c2.getId().compareTo(c1.getId()));
    }

    @Test
    void queryAgreements_verifySorting_invalidProperty() {
        IntStream.range(0, 10).forEach(i -> {
            var contractAgreement = createContractBuilder().id(i + ":" + i).build();
            var negotiation = createNegotiationBuilder(UUID.randomUUID().toString()).contractAgreement(contractAgreement).build();
            store.save(negotiation);
        });

        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();
        var agreements = store.queryAgreements(query);

        assertThat(agreements).hasSize(10);
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithAgreement() {
        var agreement = createContractBuilder().id("contract1").build();
        var negotiation = createNegotiationBuilder("negotiation1").contractAgreement(agreement).build();
        var assetId = agreement.getAssetId();

        store.save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsOnly(negotiation);
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_negotiationWithoutAgreement() {
        var assetId = UUID.randomUUID().toString();
        var negotiation = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id("negotiation1")
                .contractAgreement(null)
                .correlationId("corr-negotiation1")
                .state(REQUESTED.code())
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("protocol")
                .build();

        store.save(negotiation);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).isEmpty();
        assertThat(store.queryAgreements(QuerySpec.none())).isEmpty();
    }

    @Test
    void getNegotiationsWithAgreementOnAsset_multipleNegotiationsSameAsset() {
        var assetId = UUID.randomUUID().toString();
        var negotiation1 = createNegotiation("negotiation1", createContractBuilder("contract1").assetId(assetId).build());
        var negotiation2 = createNegotiation("negotiation2", createContractBuilder("contract2").assetId(assetId).build());

        store.save(negotiation1);
        store.save(negotiation2);

        var query = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("contractAgreement.assetId", "=", assetId)))
                .build();
        var result = store.queryNegotiations(query).collect(Collectors.toList());

        assertThat(result).hasSize(2)
                .extracting(ContractNegotiation::getId).containsExactlyInAnyOrder("negotiation1", "negotiation2");

    }

    @Override
    protected ContractNegotiationStore getContractNegotiationStore() {
        return store;
    }

    @Override
    protected void lockEntity(String negotiationId, String owner, Duration duration) {
        var document = readItem(negotiationId);
        document.acquireLease(owner, clock, duration);

        var result = container.upsertItem(document, new PartitionKey(partitionKey), new CosmosItemRequestOptions());
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Override
    protected boolean isLockedBy(String negotiationId, String owner) {
        var lease = readItem(negotiationId).getLease();
        return lease != null && lease.getLeasedBy().equals(owner) && !lease.isExpired(clock.millis());
    }

    private ContractNegotiationDocument toDocument(Object object) {
        var json = typeManager.writeValueAsString(object);
        return typeManager.readValue(json, ContractNegotiationDocument.class);
    }

    private ContractNegotiation toNegotiation(Object object) {
        return toDocument(object).getWrappedInstance();
    }

    private ContractNegotiationDocument readItem(String id) {
        var obj = container.readItem(id, new PartitionKey(partitionKey), Object.class);
        return toDocument(obj.getItem());
    }

    private void uploadStoredProcedure(CosmosContainer container, String name) {
        var sprocName = ".js";
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + sprocName);
        if (is == null) {
            throw new AssertionError("The input stream referring to the " + name + " file cannot be null!");
        }

        var s = new Scanner(is).useDelimiter("\\A");
        if (!s.hasNext()) {
            throw new IllegalArgumentException("Error loading resource with name " + sprocName);
        }
        var body = s.next();
        var props = new CosmosStoredProcedureProperties(name, body);

        var scripts = container.getScripts();
        if (scripts.readAllStoredProcedures().stream().noneMatch(sp -> sp.getId().equals(name))) {
            scripts.createStoredProcedure(props);
        }
    }

}
