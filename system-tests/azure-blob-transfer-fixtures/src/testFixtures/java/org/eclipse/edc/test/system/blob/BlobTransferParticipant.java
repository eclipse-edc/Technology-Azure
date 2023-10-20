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
import org.eclipse.edc.test.system.utils.Participant;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;

public class BlobTransferParticipant extends Participant {

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

    public Map<String, Object> getDataDestination(String transferProcessId) {
        return given()
                .baseUri(managementEndpoint.getUrl().toString())
                .contentType(ContentType.JSON)
                .when()
                .get("/v2/transferprocesses/{id}", transferProcessId)
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

        return super.requestAsset(provider, assetId, createObjectBuilder().build(), destination);
    }

    public static final class Builder extends Participant.Builder<BlobTransferParticipant, Builder> {

        private Builder() {
            super(new BlobTransferParticipant());
        }

        @Override
        public BlobTransferParticipant build() {
            super.build();
            return participant;
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }
    }
}
