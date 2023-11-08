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

import java.net.URI;
import java.util.Map;

import static java.lang.String.valueOf;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;

public interface ProviderConstants {
    String PARTICIPANT_NAME = "provider";
    String PARTICIPANT_ID = "urn:connector:provider";
    int CONNECTOR_PORT = getFreePort();
    int MANAGEMENT_PORT = getFreePort();
    String CONNECTOR_PATH = "/api";
    String MANAGEMENT_PATH = "/api/management";
    int PROTOCOL_PORT = getFreePort();
    String PROTOCOL_PATH = "/protocol";
    String PROTOCOL_URL = "http://localhost:" + PROTOCOL_PORT + PROTOCOL_PATH;
    String ASSET_PREFIX = "folderName/";
    String ASSET_FILE = "text-document.txt";
    String MANAGEMENT_URL = "http://localhost:" + MANAGEMENT_PORT + MANAGEMENT_PATH;
    URI CONTROL_URL = URI.create("http://localhost:" + getFreePort() + "/control");
    String BLOB_CONTENT = "Test blob content";
    Map<String, String> PROVIDER_PROPERTIES = Map.ofEntries(
                Map.entry("web.http.port", valueOf(ProviderConstants.CONNECTOR_PORT)),
                Map.entry("web.http.path", ProviderConstants.CONNECTOR_PATH),
                Map.entry("web.http.management.port", valueOf(ProviderConstants.MANAGEMENT_PORT)),
                Map.entry("web.http.management.path", ProviderConstants.MANAGEMENT_PATH),
                Map.entry("web.http.protocol.port", valueOf(ProviderConstants.PROTOCOL_PORT)),
                Map.entry("web.http.protocol.path", ProviderConstants.PROTOCOL_PATH),
                Map.entry("web.http.control.port", valueOf(ProviderConstants.CONTROL_URL.getPort())),
                Map.entry("web.http.control.path", ProviderConstants.CONTROL_URL.getPath()),
                Map.entry(ServiceExtensionContext.PARTICIPANT_ID, ProviderConstants.PARTICIPANT_ID),
                Map.entry("edc.dsp.callback.address", ProviderConstants.PROTOCOL_URL),
                Map.entry("edc.jsonld.http.enabled", Boolean.TRUE.toString()));
}
