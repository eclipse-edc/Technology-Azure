/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e;

import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import jakarta.json.JsonObject;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.azure.testfixtures.CosmosTestClient;
import org.eclipse.edc.azure.testfixtures.annotations.AzureCosmosDbIntegrationTest;
import org.eclipse.edc.connector.contract.spi.ContractId;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.time.Duration.ofDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_ATTRIBUTE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@AzureCosmosDbIntegrationTest
class EndToEndTransferCosmosDbTest {

    protected static final Participant CONSUMER = new Participant("consumer", "urn:connector:consumer");
    protected static final Participant PROVIDER = new Participant("provider", "urn:connector:provider");
    private static final String CONTRACT_EXPIRY_EVALUATION_KEY = EDC_NAMESPACE + "inForceDate";
    private static final String E2E_TEST_NAME = "e2e-transfer-test-" + UUID.randomUUID();
    @RegisterExtension
    static EdcRuntimeExtension consumerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-cosmosdb",
            "consumer-control-plane",
            CONSUMER.controlPlaneCosmosDbConfiguration(E2E_TEST_NAME)
    );
    @RegisterExtension
    static EdcRuntimeExtension consumerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "consumer-data-plane",
            CONSUMER.dataPlaneConfiguration()
    );
    @RegisterExtension
    static EdcRuntimeExtension consumerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "consumer-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(CONSUMER.backendService().getPort()));
                }
            }
    );
    @RegisterExtension
    static EdcRuntimeExtension providerDataPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:data-plane",
            "provider-data-plane",
            PROVIDER.dataPlaneConfiguration()
    );
    @RegisterExtension
    static EdcRuntimeExtension providerControlPlane = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:control-plane-cosmosdb",
            "provider-control-plane",
            PROVIDER.controlPlaneCosmosDbConfiguration(E2E_TEST_NAME)
    );
    @RegisterExtension
    static EdcRuntimeExtension providerBackendService = new EdcRuntimeExtension(
            ":system-tests:e2e-transfer-test:backend-service",
            "provider-backend-service",
            new HashMap<>() {
                {
                    put("web.http.port", String.valueOf(PROVIDER.backendService().getPort()));
                }
            }
    );
    private static CosmosDatabase database;
    protected final Duration timeout = Duration.ofSeconds(60);

    @BeforeAll
    static void beforeAll() {
        var client = CosmosTestClient.createClient();
        var response = client.createDatabaseIfNotExists(E2E_TEST_NAME);
        database = client.getDatabase(response.getProperties().getId());

        Stream.of("provider", "consumer")
                .flatMap(str -> Stream.of(
                        str + "-assetindex",
                        str + "-contractdefinitionstore",
                        str + "-contractnegotiationstore",
                        str + "-nodedirectory",
                        str + "-policystore",
                        str + "-transfer-process-store"))

                .map(name -> database.createContainerIfNotExists(name, "/partitionKey"))
                .map(r -> database.getContainer(r.getProperties().getId()))
                .forEach(container -> {
                    var api = new CosmosDbApiImpl(container, false);
                    api.uploadStoredProcedure("nextForState");
                    api.uploadStoredProcedure("lease");
                });

    }

    @AfterAll
    static void cleanup() {
        if (database != null) {
            CosmosDatabaseResponse delete = database.delete();
            assertThat(delete.getStatusCode()).isGreaterThanOrEqualTo(200).isLessThan(300);
        }
    }

    @Test
    void httpPullDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransferWithDynamicReceiver(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        // retrieve the data reference
        var edr = CONSUMER.getDataReference(transferProcessId);

        // pull the data without query parameter
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));

        // pull the data with additional query parameter
        var msg = UUID.randomUUID().toString();
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of("message", msg), equalTo(msg)));

        var edrList = CONSUMER.getAllDataReferences(transferProcessId);

        assertThat(edrList).hasSize(2);

    }

    @Test
    void httpPull_withExpiredContract_fixedInForcePeriod() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        var now = Instant.now();
        // contract was valid from t-10d to t-5d, so "now" it is expired
        var contractPolicy = inForcePolicy(Operator.GEQ, now.minus(ofDays(10)), Operator.LEQ, now.minus(ofDays(5)));
        createResourcesOnProvider(assetId, contractPolicy, httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(TERMINATED.name());
        });
    }

    @Test
    void httpPull_withExpiredContract_durationInForcePeriod() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        var now = Instant.now();
        // contract was valid from t-10d to t-5d, so "now" it is expired
        var contractPolicy = inForcePolicy(Operator.GEQ, now.minus(ofDays(10)), Operator.LEQ, "contractAgreement+1s");
        createResourcesOnProvider(assetId, contractPolicy, httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(TERMINATED.name());
        });
    }

    @Test
    void httpPullDataTransferProvisioner() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpProvision",
                "proxyQueryParams", "true"
        ));

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, syncDataAddress());

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        var edr = CONSUMER.getDataReference(transferProcessId);
        await().atMost(timeout).untilAsserted(() -> CONSUMER.pullData(edr, Map.of(), equalTo("some information")));
    }

    @Test
    void httpPushDataTransfer() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressProperties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);

        var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
        });
    }

    @Test
    @DisplayName("Provider pushes data to Consumer, Provider needs to authenticate the data request through an oauth2 server")
    void httpPushDataTransfer_oauth2Provisioning() {
        registerDataPlanes();
        var assetId = UUID.randomUUID().toString();
        createResourcesOnProvider(assetId, noConstraintPolicy(), httpDataAddressOauth2Properties());

        var dataset = CONSUMER.getDatasetForAsset(assetId, PROVIDER);
        var contractId = getContractId(dataset);
        var policy = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject();

        var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, contractId.toString(), contractId.assetIdPart(), policy);

        var destination = httpDataAddress(CONSUMER.backendService() + "/api/consumer/store");
        var transferProcessId = CONSUMER.initiateTransfer(contractAgreementId, assetId, PROVIDER, destination);

        await().atMost(timeout).untilAsserted(() -> {
            var state = CONSUMER.getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(STARTED.name());
        });

        await().atMost(timeout).untilAsserted(() -> {
            given()
                    .baseUri(CONSUMER.backendService().toString())
                    .when()
                    .get("/api/consumer/data")
                    .then()
                    .statusCode(anyOf(is(200), is(204)))
                    .body(is(notNullValue()));
        });
    }

    private ContractId getContractId(JsonObject dataset) {
        var id = dataset.getJsonArray(ODRL_POLICY_ATTRIBUTE).get(0).asJsonObject().getString(ID);
        return ContractId.parseId(id).orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }

    private JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                        .build())
                .build();
    }

    private JsonObject syncDataAddress() {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpProxy")
                .build();
    }

    @NotNull
    private Map<String, Object> httpDataAddressOauth2Properties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/oauth2data",
                "type", "HttpData",
                "proxyQueryParams", "true",
                "oauth2:clientId", "clientId",
                "oauth2:clientSecretKey", "provision-oauth-secret",
                "oauth2:tokenUrl", PROVIDER.backendService() + "/api/oauth2/token"
        );
    }

    @NotNull
    private Map<String, Object> httpDataAddressProperties() {
        return Map.of(
                "name", "transfer-test",
                "baseUrl", PROVIDER.backendService() + "/api/provider/data",
                "type", "HttpData",
                "proxyQueryParams", "true"
        );
    }

    private void registerDataPlanes() {
        PROVIDER.registerDataPlane();
        CONSUMER.registerDataPlane();
    }

    private void createResourcesOnProvider(String assetId, JsonObject contractPolicy, Map<String, Object> dataAddressProperties) {
        PROVIDER.createAsset(assetId, dataAddressProperties);
        var accessPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(contractPolicy);
        PROVIDER.createContractDefinition(assetId, UUID.randomUUID().toString(), accessPolicyId, contractPolicyId);
    }

    private JsonObject noConstraintPolicy() {
        return createObjectBuilder()
                .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                .add(TYPE, "use")
                .build();
    }

    private JsonObject inForcePolicy(Operator operatorStart, Object startDate, Operator operatorEnd, Object endDate) {
        return createObjectBuilder()
                .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                .add("permission", createArrayBuilder()
                        .add(permission(operatorStart, startDate, operatorEnd, endDate)))
                .build();
    }

    private JsonObject permission(Operator operatorStart, Object startDate, Operator operatorEnd, Object endDate) {
        return createObjectBuilder()
                .add("odrl:action", "USE")
                .add("odrl:constraint", createObjectBuilder()
                        .add(TYPE, "LogicalConstraint")
                        .add("odrl:and", createArrayBuilder()
                                .add(atomicConstraint(CONTRACT_EXPIRY_EVALUATION_KEY, operatorStart, startDate))
                                .add(atomicConstraint(CONTRACT_EXPIRY_EVALUATION_KEY, operatorEnd, endDate))
                                .build())
                        .build())
                .build();
    }

    private JsonObject atomicConstraint(String leftOperand, Operator operator, Object rightOperand) {
        return createObjectBuilder()
                .add(TYPE, "Constraint")
                .add("odrl:leftOperand", leftOperand)
                .add("odrl:operator", operator.name())
                .add("odrl:rightOperand", rightOperand.toString())
                .build();
    }
}
