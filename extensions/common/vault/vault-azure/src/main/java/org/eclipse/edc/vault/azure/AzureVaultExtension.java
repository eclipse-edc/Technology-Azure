/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       sovity - Add custom Azure vault settings
 *
 */

package org.eclipse.edc.vault.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

@Extension(value = AzureVaultExtension.NAME)
public class AzureVaultExtension implements ServiceExtension {

    public static final String NAME = "Azure Vault";

    @Setting("Name of the Azure Vault")
    private static final String VAULT_NAME = "edc.vault.name";

    @Setting("If valid, ignore " + VAULT_NAME + " and use this URL as an Azure vault.")
    private static final String VAULT_URL_OVERRIDE = "edc.vault.url.override";

    @Setting(value = "If true, allow the usage of non-azure domains for the vault.", type = "boolean", defaultValue = "false")
    private static final String VAULT_URL_OVERRIDE_UNSAFE = "edc.vault.url.override.unsafe";

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault createVault(ServiceExtensionContext context) {
        var config = context.getConfig();
        var override = config.getString(VAULT_URL_OVERRIDE);

        if (override != null && !override.isEmpty()) {
            return createCustomVault(config, new SecretClientBuilder());
        } else {
            return createDefaultVault(config);
        }
    }

    @NotNull
    private AzureVault createDefaultVault(Config config) {
        var name = config.getString(VAULT_NAME);
        var credentials = new DefaultAzureCredentialBuilder().build();
        var client = new SecretClientBuilder()
                .vaultUrl("https://" + name + ".vault.azure.net")
                .credential(credentials)
                .buildClient();

        return new AzureVault(monitor, client);
    }

    @NotNull
    public Vault createCustomVault(Config config, SecretClientBuilder builder) {
        var override = config.getString(VAULT_URL_OVERRIDE);
        var useUnsafe = config.getBoolean(VAULT_URL_OVERRIDE_UNSAFE);
        var credentials = new DefaultAzureCredentialBuilder().build();

        try {
            new URL(override).toURI();
        } catch (MalformedURLException e) {
            throw new EdcException("Invalid URL '" + override + "' for setting key " + VAULT_URL_OVERRIDE, e);
        }

        builder.vaultUrl(override).credential(credentials);
        if (useUnsafe) {
            builder.disableChallengeResourceVerification();
        }

        return new AzureVault(monitor, builder.buildClient());
    }
}
