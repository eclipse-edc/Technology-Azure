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

package org.eclipse.edc.connector.store.azure.cosmos.assetindex;

import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.SqlQuerySpec;
import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosDbApi;
import org.eclipse.edc.connector.store.azure.cosmos.assetindex.model.AssetDocument;
import org.eclipse.edc.junit.matchers.PredicateMatcher;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CosmosAssetIndexTest {

    private static final String TEST_PARTITION_KEY = "test-partition-key";
    private static final String TEST_ID = "id-test";
    private CosmosDbApi api;
    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;
    private CosmosAssetIndex assetIndex;

    private static AssetDocument createDocument(String id) {
        return new AssetDocument(Asset.Builder.newInstance().id(id).build(), "partitionkey-test");
    }

    @BeforeEach
    public void setUp() {
        typeManager = new TypeManager();
        typeManager.registerTypes(AssetDocument.class, Asset.class);
        retryPolicy = RetryPolicy.builder().withMaxRetries(1).build();
        api = mock(CosmosDbApi.class);
        assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy, mock(Monitor.class));
    }

    @Test
    void inputValidation() {
        // null cosmos api
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(null, TEST_PARTITION_KEY, null, retryPolicy, mock(Monitor.class)));

        // type manager is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(api, TEST_PARTITION_KEY, null, retryPolicy, mock(Monitor.class)));

        // retry policy is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, null, mock(Monitor.class)));
    }

    @Test
    void findById() {

        AssetDocument document = createDocument(TEST_ID);
        when(api.queryItemById(TEST_ID)).thenReturn(document);

        Asset actualAsset = assetIndex.findById(TEST_ID);

        assertThat(actualAsset.getProperties()).isEqualTo(document.getWrappedInstance().getProperties());
        verify(api).queryItemById(TEST_ID);
    }

    @Test
    void findByIdThrowEdcException() {
        String id = "id-test";
        when(api.queryItemById(eq(id)))
                .thenThrow(new EdcException("Failed to fetch object"))
                .thenThrow(new EdcException("Failed again to find object"));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> assetIndex.findById(id));
        verify(api, atLeastOnce()).queryItemById(eq(id));
        verifyNoMoreInteractions(api);
    }

    @Test
    void findByIdReturnsNull() {
        String id = "id-test";
        when(api.queryItemById(eq(id))).thenReturn(null);

        Asset actualAsset = assetIndex.findById(id);

        assertThat(actualAsset).isNull();
        verify(api).queryItemById(eq(id));
        verifyNoMoreInteractions(api);
    }

    @Test
    void findAll_noQuerySpec() {
        AssetDocument document = createDocument(TEST_ID);
        var expectedQuery = "SELECT * FROM AssetDocument OFFSET 0 LIMIT 50";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.none()).collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedInstance().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedInstance().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_withPaging_SortingDesc() {
        AssetDocument document = createDocument(TEST_ID);
        var expectedQuery = "SELECT * FROM AssetDocument ORDER BY AssetDocument.wrappedInstance.anyField DESC OFFSET 5 LIMIT 100";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .offset(5)
                        .limit(100)
                        .sortField("anyField")
                        .sortOrder(SortOrder.DESC)
                        .build())
                .collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedInstance().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedInstance().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_withPaging_SortingAsc() {
        AssetDocument document = createDocument(TEST_ID);
        var expectedQuery = "SELECT * FROM AssetDocument ORDER BY AssetDocument.wrappedInstance.anyField ASC OFFSET 5 LIMIT 100";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .offset(5)
                        .limit(100)
                        .sortField("anyField")
                        .sortOrder(SortOrder.ASC)
                        .build())
                .collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedInstance().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedInstance().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_withFiltering() {
        AssetDocument document = createDocument(TEST_ID);
        var expectedQuery = "SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.someField = @someField OFFSET 5 LIMIT 100";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .offset(5)
                        .limit(100)
                        .filter(criterion("someField", "=", "randomValue"))
                        .build())
                .collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedInstance().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedInstance().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void deleteById_whenPresent_deletesItem() {
        AssetDocument document = createDocument(TEST_ID);
        when(api.deleteItem(TEST_ID)).thenReturn(document);

        var deletedAsset = assetIndex.deleteById(TEST_ID);
        assertThat(deletedAsset.succeeded()).isTrue();
        assertThat(deletedAsset.getContent().getProperties()).isEqualTo(document.getWrappedInstance().getProperties());
        verify(api).deleteItem(TEST_ID);
    }

    @Test
    void deleteById_whenMissing_returnsNotFound() {
        var id = "not-exists";
        when(api.deleteItem(id)).thenThrow(new NotFoundException());
        assertThat(assetIndex.deleteById(id)).isNotNull()
                .extracting(StoreResult::reason).isEqualTo(NOT_FOUND);
    }

    @NotNull
    private PredicateMatcher<SqlQuerySpec> queryMatches(String expectedQuery) {
        return new PredicateMatcher<>(sqlQuerySpec -> sqlQuerySpec.getQueryText().equals(expectedQuery));
    }

}
