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
 *       Marco Primo
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import org.eclipse.edc.util.string.StringUtils;

public class DestinationBlobName {

    private final String folderName;
    private final String blobName;

    public DestinationBlobName(String blobName, String folderName) {

        this.blobName = blobName;
        this.folderName = folderName;
    }

    public String resolve(String partName, int partsSize) {

        var name = (partsSize == 1 && !StringUtils.isNullOrEmpty(this.blobName)) ? this.blobName : partName;
        if (!StringUtils.isNullOrEmpty(this.folderName)) {
            name = this.folderName.endsWith("/") ? this.folderName + name : this.folderName + "/" + name;
        }
        return name;
    }

}

