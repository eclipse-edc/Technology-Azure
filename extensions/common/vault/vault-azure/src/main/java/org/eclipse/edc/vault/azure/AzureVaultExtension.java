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
 *
 */

package org.eclipse.edc.vault.azure;

import com.azure.core.annotation.ServiceClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.NotNull;

@Extension(value = AzureVaultExtension.NAME)
public class AzureVaultExtension implements ServiceExtension {

    public static final String NAME = "Azure Vault";

    @Setting("Name of the Azure Vault")
    private static final String VAULT_NAME = "edc.vault.name";

    @Setting("If non blank, ignore edc.vault.name and use this value as an Azure vault.")
    private static final String VAULT_OVERRIDE = "edc.vault.override";

    @Setting(value = "If true, allow the usage of non-azure domains for the vault.", type = "boolean")
    private static final String VAULT_OVERRIDE_UNSAFE = "edc.vault.override.unsafe";

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public Vault createVault(ServiceExtensionContext context) {
        Config config = context.getConfig();
        var override = config.getString(VAULT_OVERRIDE);

        if (override != null && !override.isEmpty()) {
            return createCustomVault(config, new SecretClientBuilder());
        } else {
            return createVault(config);
        }

    }

    @NotNull
    private AzureVault createVault(Config config) {
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
        var override = config.getString(VAULT_OVERRIDE);
        var useUnsafe = config.getBoolean(VAULT_OVERRIDE_UNSAFE);
        var credentials = new DefaultAzureCredentialBuilder().build();

        builder.vaultUrl(override).credential(credentials);
        if (useUnsafe) {
            builder.disableChallengeResourceVerification();
        }

        return new AzureVault(monitor, builder.buildClient());
    }
}
