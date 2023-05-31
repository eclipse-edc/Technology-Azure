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

import org.eclipse.edc.registration.spi.model.Participant;
import org.eclipse.edc.registration.spi.model.ParticipantStatus;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantDocumentSerializationTest {

    private TypeManager typeManager;

    private static Participant createParticipant() {
        return Participant.Builder.newInstance()
                .id("id-test")
                .did("did-test")
                .state(ParticipantStatus.ONBOARDED.code())
                .build();
    }

    @BeforeEach
    void setup() {
        typeManager = new TypeManager();
        typeManager.registerTypes(ParticipantDocument.class, Participant.class);
    }

    @Test
    void testSerialization() {
        var participant = createParticipant();

        var document = new ParticipantDocument(participant, "partitionkey-test");

        String s = typeManager.writeValueAsString(document);

        assertThat(s).isNotNull()
                .contains("\"partitionKey\":\"partitionkey-test\"")
                .contains("\"id\":\"id-test\"")
                .contains("\"did\":\"did-test\"")
                .contains("\"state\":300")
                .contains("\"wrappedInstance\":");
    }

    @Test
    void testDeserialization() {
        var participant = createParticipant();

        var document = new ParticipantDocument(participant, "partitionkey-test");
        String json = typeManager.writeValueAsString(document);

        var deserialized = typeManager.readValue(json, ParticipantDocument.class);
        assertThat(deserialized.getWrappedInstance()).usingRecursiveComparison().isEqualTo(document.getWrappedInstance());
    }
}
