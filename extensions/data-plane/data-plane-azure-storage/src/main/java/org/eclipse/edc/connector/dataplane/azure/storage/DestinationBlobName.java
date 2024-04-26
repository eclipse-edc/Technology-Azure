/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Marco Pirmo (BMW AG)
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import org.eclipse.edc.util.string.StringUtils;

import java.util.Optional;

/**
 * Utility class responsible for determining the name under which a file will be saved.
 */
public class DestinationBlobName {

    private final Optional<String> folderName;
    private final Optional<String> blobName;
    private static final String PARTNAME_VALIDATION_MESSAGE = "partName cannot be null or blank when blobName is empty or not provided.";


    public DestinationBlobName(String blobName, String folderName) {

        this.blobName = Optional.ofNullable(blobName);
        this.folderName = Optional.ofNullable(folderName);
    }

    /**
     * Resolves the name under which a resource should be saved based on its part name and size.
     *
     * @param partName  The name of the resource part.
     * @param partsSize The size of the resource part.
     * @return A String representing the resolved name for the resource.
     */
    public String resolve(String partName, int partsSize) {

        var sb = new StringBuilder();
        if (blobName.isEmpty() || blobName.get().isBlank()) {
            if (StringUtils.isNullOrBlank(partName)) {
                throw new IllegalArgumentException(PARTNAME_VALIDATION_MESSAGE);
            }
        }
        if (folderName.isPresent() && !folderName.get().isBlank()) {
            if (folderName.get().endsWith("/")) {
                sb.append(folderName.get());
            } else {
                sb.append(folderName.get()).append("/");
            }
        }
        if (partsSize == 1 && blobName.isPresent() && !blobName.get().isBlank()) {
            sb.append(blobName.get());
        } else {
            sb.append(partName);
        }

        return sb.toString();
    }

}

