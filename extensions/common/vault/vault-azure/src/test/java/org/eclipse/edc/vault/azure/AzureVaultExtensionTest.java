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

package org.eclipse.edc.vault.azure;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
public class AzureVaultExtensionTest {

    private static final String VAULT_NAME = "aVault";
    private static final String VAULT_NAME_SETTING = "edc.vault.name";

    private ServiceExtensionContext context;
    private AzureVaultExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = context;
        extension = factory.constructInstance(AzureVaultExtension.class);
    }

    @Test
    void verifyInitialize() {
        extension.initialize(context);
        assertThat(context.getService(Vault.class)).isInstanceOf(AzureVault.class);
    }

    @BeforeAll
    static void setProps() {
        System.setProperty(VAULT_NAME_SETTING, VAULT_NAME);
    }

    @AfterAll
    static void unsetProps() {
        System.clearProperty(VAULT_NAME_SETTING);
    }
}
