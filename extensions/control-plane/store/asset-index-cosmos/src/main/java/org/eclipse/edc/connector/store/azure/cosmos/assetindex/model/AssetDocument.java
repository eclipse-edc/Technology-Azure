/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.store.azure.cosmos.assetindex.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.edc.azure.cosmos.CosmosDocument.unsanitize;
import static org.eclipse.edc.spi.types.domain.asset.Asset.PROPERTY_ID;

@JsonTypeName("dataspaceconnector:assetdocument")
public class AssetDocument extends CosmosDocument<Asset> {
    private final String id;

    @JsonCreator
    public AssetDocument(@JsonProperty("wrappedInstance") Asset wrappedInstance,
                         @JsonProperty("partitionKey") String partitionKey) {
//        super(sanitizeProperties(wrappedInstance), partitionKey);
        super(wrappedInstance, partitionKey);
        id = wrappedInstance.getId();
    }

    private static Asset sanitizeProperties(Asset asset) {
        var properties = asset.getProperties();
        properties.remove(PROPERTY_ID);
        return Asset.Builder.newInstance()
                .id(asset.getId())
                .properties(sanitize(properties))
                .privateProperties(sanitize(asset.getPrivateProperties()))
                .dataAddress(asset.getDataAddress())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    @Override
    public String getId() {
        return id;
    }

    public Asset getAsset() {
//        var asset = getWrappedInstance();
//        var properties = asset.getProperties();
//        properties.remove(unsanitize(PROPERTY_ID));
//        return Asset.Builder.newInstance()
//                .id(asset.getId())
//                .properties(unsanitize(properties))
//                .privateProperties(unsanitize(asset.getPrivateProperties()))
//                .dataAddress(asset.getDataAddress())
//                .createdAt(asset.getCreatedAt())
//                .build();
        return getWrappedInstance();
    }

    @NotNull
    private static Map<String, Object> sanitize(Map<String, Object> properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(entry -> sanitize(entry.getKey()), Map.Entry::getValue));
    }

    @NotNull
    private static Map<String, Object> unsanitize(Map<String, Object> properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(entry -> unsanitize(entry.getKey()), Map.Entry::getValue));
    }

}
