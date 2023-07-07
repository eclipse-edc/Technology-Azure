/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.azure.cosmos.dialect;

import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.azure.cosmos.dialect.CosmosConstants.ORDER_BY;
import static org.eclipse.edc.azure.cosmos.dialect.CosmosConstants.hasIllegalCharacters;

/**
 * Represents in a structural way an ORDER BY clause in an SQL statement.
 * The sort criterion, i.e. the SQL column by which to sort, is needed.
 * <p>
 * Optionally an {@code orderPrefix} can be specified, which represents the "path" of the property. This is particularly
 * relevant for CosmosDB queries, e.g:
 * <pre>
 *     SELECT * FROM YourDocument WHERE ... ORDER BY YourDocument.Header.Age
 * </pre>
 * In this case the {@code orderPrefix} would have to be {@code "YourDocument.Header"}.
 */
class OrderByClause implements Clause {

    private final String orderField;
    private final boolean sortAsc;
    private final String objectPrefix;

    OrderByClause(String orderField, boolean sortAsc, String objectPrefix) {

        this.orderField = orderField;
        this.sortAsc = sortAsc;
        this.objectPrefix = objectPrefix;
    }

    OrderByClause(String orderField, boolean sortAsc) {
        this(orderField, sortAsc, null);
    }

    OrderByClause() {
        this(null, true, null);
    }

    @Override
    public String asString() {
        return orderField != null ? getOrderByExpression() : "";
    }

    private String getOrderByExpression() {
        if (hasIllegalCharacters(orderField) && !orderField.contains("[")) {
            var pfx = objectPrefix != null ? objectPrefix : "";
            return format(ORDER_BY + " %s[\"%s\"] %s", pfx, orderField, sortAsc ? "ASC" : "DESC");
        } else {
            var pfx = objectPrefix != null ? objectPrefix + "." : "";
            return format(ORDER_BY + " %s%s %s", pfx, orderField, sortAsc ? "ASC" : "DESC");
        }
    }

    @NotNull
    private String getPrefix() {
        return objectPrefix != null ? objectPrefix + "." : "";
    }

}
