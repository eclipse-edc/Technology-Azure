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

import java.net.URI;

import static org.eclipse.edc.util.io.Ports.getFreePort;


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
}
