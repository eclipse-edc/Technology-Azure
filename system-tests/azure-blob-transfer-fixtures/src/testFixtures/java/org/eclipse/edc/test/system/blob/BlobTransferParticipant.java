/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.test.system.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.restassured.http.ContentType;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.valueOf;
import static java.util.Map.entry;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class BlobTransferParticipant extends Participant {

    private final String containerName = UUID.randomUUID().toString();

    public Config createConfig(int blobStoragePort) {
        return ConfigFactory.fromMap(Map.ofEntries(
                entry("edc.participant.id", id),
                entry("edc.blobstore.endpoint.template", "http://127.0.0.1:" + blobStoragePort + "/%s"),
                entry("edc.test.asset.container.name", containerName),
                entry("web.http.port", String.valueOf(getFreePort())),
                entry("web.http.path", "/"),
                entry("web.http.management.port", valueOf(controlPlaneManagement.get().getPort())),
                entry("web.http.management.path", controlPlaneManagement.get().getPath()),
                entry("web.http.protocol.port", valueOf(controlPlaneProtocol.get().getPort())),
                entry("web.http.protocol.path", controlPlaneProtocol.get().getPath()),
                entry("web.http.control.port", valueOf(getFreePort())),
                entry("web.http.control.path", "/control"),
                entry("edc.dsp.callback.address", controlPlaneProtocol.get().toString()),
                entry("edc.transfer.proxy.token.verifier.publickey.alias", "test-alias"),
                entry("edc.transfer.proxy.token.signer.privatekey.alias", "test-private-alias"),
                entry("edc.jsonld.http.enabled", Boolean.TRUE.toString())
        ));
    }

    public String getContainerName() {
        return containerName;
    }

    public String createBlobAsset(String accountName, String containerName, String blobName) {
        var assetId = UUID.randomUUID().toString();

        Map<String, Object> dataAddressProperties = Map.of(
                "type", AzureBlobStoreSchema.TYPE,
                AzureBlobStoreSchema.ACCOUNT_NAME, accountName,
                AzureBlobStoreSchema.CONTAINER_NAME, containerName,
                AzureBlobStoreSchema.BLOB_NAME, blobName,
                "keyName", accountName
        );

        Map<String, Object> properties = Map.of(
                "name", assetId,
                "contenttype", "text/plain",
                "version", "1.0"
        );

        return createAsset(assetId, properties, dataAddressProperties);
    }

    public String createBlobInFolderAsset(String accountName, String containerName, String blobPrefix) {
        var assetId = UUID.randomUUID().toString();

        Map<String, Object> dataAddressProperties = Map.of(
                "type", AzureBlobStoreSchema.TYPE,
                AzureBlobStoreSchema.ACCOUNT_NAME, accountName,
                AzureBlobStoreSchema.CONTAINER_NAME, containerName,
                AzureBlobStoreSchema.BLOB_PREFIX, blobPrefix,
                "keyName", accountName
        );

        Map<String, Object> properties = Map.of(
                "name", assetId,
                "contenttype", "text/directory",
                "version", "1.0"
        );

        return createAsset(assetId, properties, dataAddressProperties);
    }

    public Map<String, Object> getDataDestination(String transferProcessId) {
        return baseManagementRequest()
                .contentType(ContentType.JSON)
                .when()
                .get("/transferprocesses/{id}", transferProcessId)
                .then()
                .statusCode(200)
                .extract().jsonPath().get("'dataDestination'");
    }

    public String requestAssetAndTransferToBlob(Participant provider, String assetId, String accountName) {
        return requestAssetAndTransferToBlob(provider, assetId, accountName, null);
    }

    public String requestAssetAndTransferToBlob(Participant provider, String assetId, String accountName, String containerName) {
        var destinationProps = createObjectBuilder().add(AzureBlobStoreSchema.ACCOUNT_NAME, accountName);
        if (containerName != null) {
            destinationProps.add(AzureBlobStoreSchema.CONTAINER_NAME, containerName);
        }
        var destination = createObjectBuilder()
                .add("type", AzureBlobStoreSchema.TYPE)
                .add("properties", destinationProps)
                .build();

        return this.requestAssetFrom(assetId, provider)
                .withPrivateProperties(createObjectBuilder().build())
                .withDestination(destination)
                .withTransferType(AzureBlobStoreSchema.TRANSFERTYPE_PUSH)
                .execute();
    }

    public static final class Builder extends Participant.Builder<BlobTransferParticipant, Builder> {

        private Builder() {
            super(new BlobTransferParticipant());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder controlManagementEndpoint(String managementUrl) {
            participant.controlPlaneManagement = new LazySupplier<>(() -> URI.create(managementUrl));
            return this;
        }

        public Builder controlProtocolEndpoint(String protocolUrl) {
            participant.controlPlaneProtocol = new LazySupplier<>(() -> URI.create(protocolUrl));
            return this;
        }
    }
}
