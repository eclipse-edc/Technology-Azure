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
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class AzureVaultExtensionTest {

    private static final String VAULT_NAME = "aVault";
    private static final String VAULT_NAME_SETTING = "edc.vault.name";


    @Test
    void verifyCreateVault(AzureVaultExtension extension, ServiceExtensionContext context) {
        Config cfg = mock();
        when(cfg.getString(VAULT_NAME_SETTING)).thenReturn(VAULT_NAME);
        when(context.getConfig()).thenReturn(cfg);


        assertThat(extension.createVault(context)).isInstanceOf(AzureVault.class);
        verify(context, atLeastOnce()).getConfig();
        verify(cfg).getString(VAULT_NAME_SETTING);

    }
}
