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

package org.eclipse.edc.azure.blob;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SecretToken;

@JsonTypeName("dataspaceconnector:azuretoken")
public class AzureSasToken implements SecretToken {
    private final String sas;
    private final long expiration;

    public AzureSasToken(@JsonProperty("sas") String writeOnlySas, @JsonProperty("expiration") long expiration) {
        sas = writeOnlySas;
        this.expiration = expiration;
    }

    public String getSas() {
        return sas;
    }

    @Override
    public long getExpiration() {
        return expiration;
    }
}
