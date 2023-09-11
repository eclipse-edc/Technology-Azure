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

import com.azure.core.credential.TokenCredential;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.CertificateResolver;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.security.VaultCertificateResolver;
import org.eclipse.edc.spi.security.VaultPrivateKeyResolver;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Provides({Vault.class, PrivateKeyResolver.class, CertificateResolver.class})
@Extension(value = AzureVaultExtension.NAME)
public class AzureVaultExtension implements ServiceExtension {

    public static final String NAME = "Azure Vault";
    @Setting
    private static final String VAULT_NAME = "edc.vault.name";

    @Inject
    private Monitor monitor;

    @Inject
    private TokenCredential tokenCredential;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var name = context.getConfig().getString(VAULT_NAME);
        var client = new SecretClientBuilder()
                .vaultUrl("https://" + name + ".vault.azure.net")
                .credential(tokenCredential)
                .buildClient();
        var vault = new AzureVault(context.getMonitor(), client);

        context.registerService(Vault.class, vault);
        context.registerService(PrivateKeyResolver.class, new VaultPrivateKeyResolver(vault));
        context.registerService(CertificateResolver.class, new VaultCertificateResolver(vault));
    }
}
