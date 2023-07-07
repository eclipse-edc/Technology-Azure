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

import com.azure.cosmos.models.SqlParameter;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.spi.query.Criterion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents in a structural way a WHERE clause in an SQL statement. Attempts to use parameterized statements using
 * {@link SqlParameter} if possible. Currently, this is only implemented for the equals-operator ("=").
 * <p>
 * For every {@link Criterion} that is passed in, another {@code WHERE}- or {@code AND}-clause is appended.
 *
 * <p>
 * Optionally an {@code orderPrefix} can be specified, which represents the "path" of the property. This is particularly
 * relevant for CosmosDB queries, e.g:
 * <pre>
 *     SELECT * FROM YourDocument WHERE YourDocument.Header.Author = 'Foo Bar'
 * </pre>
 * In this case the {@code orderPrefix} would have to be {@code "YourDocument.Header"}.
 */
class WhereClause implements Clause {
    private final String objectPrefix;
    private final List<SqlParameter> parameters = new ArrayList<>();
    private final Class<? extends CosmosDocument<?>> target;

    private ConditionExpressionParser parser = new ConditionExpressionParser();
    private String where = "";

    WhereClause() {
        objectPrefix = null;
        target = null;
    }

    WhereClause(Class<? extends CosmosDocument<?>> target, List<Criterion> criteria, String objectPrefix) {
        this.objectPrefix = objectPrefix;
        this.target = target;
        parser = new ConditionExpressionParser(target);
        if (criteria != null) {
            criteria.stream().distinct().forEach(this::parse);
        }
    }

    WhereClause(List<Criterion> criteria, String objectPrefix) {
        this(null, criteria, objectPrefix);
    }

    @Override
    public String asString() {
        return getWhere();
    }

    @Override
    public @NotNull List<SqlParameter> getParameters() {
        return parameters;
    }

    String getWhere() {
        return where;
    }


    private void parse(Criterion criterion) {
        var expr = parser.parse(criterion, objectPrefix);
        var exprResult = expr.isValidExpression();
        if (exprResult.failed()) {
            throw new IllegalArgumentException("Cannot build WHERE clause, reason: " + String.join(", ", exprResult.getFailureMessages()));
        }

        where += where.startsWith(CosmosConstants.WHERE) ? " " + CosmosConstants.AND : CosmosConstants.WHERE; //if we have a chained WHERE ... AND ... statement
        addParametersWithIndex(expr.getParameters());
        where += expr.toExpressionString();

    }

    /**
     * add a statement parameter. If a statement parameter with the same name already exists, a number is appended, e.g.:
     * \@myParam exists, then adding a parameter with the same name would add \@myParam1, then \@myParam2, etc.
     */
    private void addParametersWithIndex(List<SqlParameter> newParams) {
        for (var newParam : newParams) {

            var counter = 0;
            var name = newParam.getName().replace("\"", "_");
            var newName = name;
            for (var existinParam : parameters) {
                while (existinParam.getName().equals(newName)) {
                    counter++;
                    newName = name + "_" + counter;
                }
            }
            newParam.setName(newName);
            parameters.add(newParam);

        }
    }

}
