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

package org.eclipse.edc.azure.testfixtures;

import org.eclipse.edc.junit.testfixtures.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AzureSettings {

    public static final String AZURE_SETTINGS_FILE = "resources/azure/testing/runtime_settings.properties";

    private final Properties properties;

    public AzureSettings() {
        var absolutePath = azureSettingsFileAbsolutePath();
        try (var input = new FileInputStream(absolutePath)) {
            properties = new Properties();
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error in loading runtime settings properties", e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String azureSettingsFileAbsolutePath() {
        return new File(TestUtils.findBuildRoot(), AZURE_SETTINGS_FILE).getAbsolutePath();
    }
}
