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

import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public interface ConsumerConstants {
    String PARTICIPANT_NAME = "consumer";
    String PARTICIPANT_ID = "urn:connector:consumer";

    int CONNECTOR_PORT = getFreePort();
    int MANAGEMENT_PORT = getFreePort();
    String CONNECTOR_PATH = "/api";
    String MANAGEMENT_PATH = "/api/management";
    String MANAGEMENT_URL = "http://localhost:" + MANAGEMENT_PORT + MANAGEMENT_PATH;
    int PROTOCOL_PORT = getFreePort();
    String PROTOCOL_PATH = "/protocol";
    String PROTOCOL_URL = "http://localhost:" + PROTOCOL_PORT + PROTOCOL_PATH;
    Map<String, String> CONSUMER_PROPERTIES = new HashMap<>() {{
                put("web.http.port", valueOf(ConsumerConstants.CONNECTOR_PORT));
                put("web.http.path", ConsumerConstants.CONNECTOR_PATH);
                put("web.http.management.port", valueOf(ConsumerConstants.MANAGEMENT_PORT));
                put("web.http.management.path", ConsumerConstants.MANAGEMENT_PATH);
                put("web.http.protocol.port", valueOf(ConsumerConstants.PROTOCOL_PORT));
                put("web.http.protocol.path", ConsumerConstants.PROTOCOL_PATH);
                put(ServiceExtensionContext.PARTICIPANT_ID, ConsumerConstants.PARTICIPANT_ID);
                put("edc.dsp.callback.address", ConsumerConstants.PROTOCOL_URL);
                put("edc.jsonld.http.enabled", Boolean.TRUE.toString());
            }};
}
