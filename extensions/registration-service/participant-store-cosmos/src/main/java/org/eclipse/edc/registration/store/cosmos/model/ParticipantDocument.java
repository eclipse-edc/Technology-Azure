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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.registration.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.registration.spi.model.Participant;


@JsonTypeName("dataspaceconnector:participantdocument")
public class ParticipantDocument extends CosmosDocument<Participant> {

    @JsonCreator
    public ParticipantDocument(@JsonProperty("wrappedInstance") Participant participant,
                               @JsonProperty("partitionKey") String partitionKey) {
        super(participant, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }
}
