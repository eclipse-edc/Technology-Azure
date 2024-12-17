/*
 *  Copyright (c) 2024 Bayerische Motorenwerke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.azure;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public record AzureProvisionConfiguration(
        @Setting(key = "edc.azure.token.expiry.time", description = "Expiration time, in hours, for the SAS token.", defaultValue = "1") long tokenExpiryTime) {
}
