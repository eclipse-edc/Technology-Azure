/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.azure.testfixtures;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public class AzuriteExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String IMAGE_NAME = "mcr.microsoft.com/azure-storage/azurite:3.33.0";

    private final AzuriteContainer azuriteContainer;

    public AzuriteExtension(int azuriteHostPort, Account... accounts) {
        azuriteContainer = new AzuriteContainer(azuriteHostPort, accounts);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        azuriteContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        azuriteContainer.stop();
    }

    public record Account(String name, String key) { }

    private static class AzuriteContainer extends GenericContainer<AzuriteContainer> {

        AzuriteContainer(int azuriteHostPort, Account... accounts) {
            super(IMAGE_NAME);
            addEnv("AZURITE_ACCOUNTS", stream(accounts).map(it -> "%s:%s".formatted(it.name(), it.key())).collect(joining(";")));
            setPortBindings(List.of("%d:%d".formatted(azuriteHostPort, 10_000)));
        }

    }
}
