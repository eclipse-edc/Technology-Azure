/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.azure.testfixtures.annotations;

import org.eclipse.edc.junit.annotations.IntegrationTest;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for Azure Cosmos DB integration testing.It applies specific Junit Tag.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Tag("PostgresCosmosTest")
public @interface PostgresCosmosTest {
}
