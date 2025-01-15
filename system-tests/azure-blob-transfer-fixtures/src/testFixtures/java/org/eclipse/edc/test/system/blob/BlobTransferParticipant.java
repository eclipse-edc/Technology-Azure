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
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class BlobTransferParticipant extends Participant {

    private final String containerName = UUID.randomUUID().toString();

    public Config createConfig(int blobStoragePort) {
        return ConfigFactory.fromMap(Map.ofEntries(
                Map.entry("edc.blobstore.endpoint.template", "http://127.0.0.1:" + blobStoragePort + "/%s"),
                Map.entry("edc.test.asset.container.name", containerName),
                Map.entry("web.http.port", String.valueOf(getFreePort())),
                Map.entry("web.http.path", "/"),
                Map.entry("web.http.management.port", valueOf(controlPlaneManagement.get().getPort())),
                Map.entry("web.http.management.path", controlPlaneManagement.get().getPath()),
                Map.entry("web.http.protocol.port", valueOf(controlPlaneProtocol.get().getPort())),
                Map.entry("web.http.protocol.path", controlPlaneProtocol.get().getPath()),
                Map.entry("web.http.control.port", valueOf(getFreePort())),
                Map.entry("web.http.control.path", "/control"),
                Map.entry(PARTICIPANT_ID, id),
                Map.entry("edc.dsp.callback.address", controlPlaneProtocol.get().toString()),
                Map.entry("edc.transfer.proxy.token.verifier.publickey.alias", "test-alias"),
                Map.entry("edc.transfer.proxy.token.signer.privatekey.alias", "test-private-alias"),
                Map.entry("edc.jsonld.http.enabled", Boolean.TRUE.toString())
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
                "keyName", format("%s-key1", accountName)
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
                "keyName", format("%s-key1", accountName)
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
                .get("/v3/transferprocesses/{id}", transferProcessId)
                .then()
                .statusCode(200)
                .extract().jsonPath().get("'dataDestination'");
    }

    public String requestAssetAndTransferToBlob(Participant provider, String assetId, String accountName) {
        var destination = createObjectBuilder()
                .add("type", AzureBlobStoreSchema.TYPE)
                .add("properties", createObjectBuilder()
                        .add(AzureBlobStoreSchema.ACCOUNT_NAME, accountName))
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

    }
}
