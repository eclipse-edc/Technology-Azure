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

import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class AzureVaultExtensionTest {

    private static final String VAULT_NAME = "aVault";
    private static final String VAULT_NAME_SETTING = "edc.vault.name";
    private static final String VAULT_NAME_OVERRIDE_SETTING = "edc.vault.url.override";
    private static final String VAULT_NAME_OVERRIDE_UNSAFE_SETTING = "edc.vault.url.override.unsafe";


    @Test
    void verifyCreateVault(AzureVaultExtension extension, ServiceExtensionContext context) {
        Config cfg = mock();
        when(cfg.getString(VAULT_NAME_SETTING)).thenReturn(VAULT_NAME);
        when(context.getConfig()).thenReturn(cfg);

        assertThat(extension.createVault(context)).isInstanceOf(AzureVault.class);
        verify(context, atLeastOnce()).getConfig();
        verify(cfg).getString(VAULT_NAME_SETTING);

    }

    @Test
    void createVault_whenConfiguredWithInvalidUrl_shouldRefuseInvalidUrls(AzureVaultExtension extension, ServiceExtensionContext context) {
        Config cfg = mock();
        when(cfg.getString(VAULT_NAME_OVERRIDE_SETTING)).thenReturn("not a valid URL");
        when(context.getConfig()).thenReturn(cfg);

        Assertions.assertThrows(EdcException.class, () -> extension.createVault(context));
    }

    @Test
    void createCustomVault_whenConfiguredWithOverride_shouldNotBeUnsafeByDefault(AzureVaultExtension extension, ServiceExtensionContext context) {
        var builder = spy(new SecretClientBuilder());
        Config cfg = mockConfiguration(context);
        when(cfg.getBoolean(VAULT_NAME_OVERRIDE_UNSAFE_SETTING)).thenReturn(false);

        extension.createCustomVault(cfg, builder);
        assertConfigUsageForCustomVault(context, cfg);
        verify(builder, never()).disableChallengeResourceVerification();
    }

    @Test
    void createCustomVault_whenConfiguredWithUnsafeOverride_shouldUseAnyValue(AzureVaultExtension extension, ServiceExtensionContext context) {
        var builder = spy(new SecretClientBuilder());
        var cfg = mockConfiguration(context);
        when(cfg.getBoolean(VAULT_NAME_OVERRIDE_UNSAFE_SETTING)).thenReturn(true);

        extension.createCustomVault(cfg, builder);
        assertConfigUsageForCustomVault(context, cfg);
        verify(builder).disableChallengeResourceVerification();
    }

    @NotNull
    private static Config mockConfiguration(ServiceExtensionContext context) {
        Config cfg = mock();
        when(cfg.getString(VAULT_NAME_OVERRIDE_SETTING)).thenReturn("http://example.com");
        when(context.getConfig()).thenReturn(cfg);
        return cfg;
    }

    private static void assertConfigUsageForCustomVault(ServiceExtensionContext context, Config cfg) {
        verify(context, atLeastOnce()).getConfig();
        verify(cfg, atLeastOnce()).getString(VAULT_NAME_OVERRIDE_SETTING);
        verify(cfg, atLeastOnce()).getBoolean(VAULT_NAME_OVERRIDE_UNSAFE_SETTING);
        verify(cfg, never()).getString(VAULT_NAME);
    }
}
