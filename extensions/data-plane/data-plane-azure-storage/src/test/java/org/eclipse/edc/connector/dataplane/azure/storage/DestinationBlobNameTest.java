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
 *       Marco Primo
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class DestinationBlobNameTest {


    @ParameterizedTest
    @CsvSource(value = {
            "partName, blobName,  null, 1, blobName",
            "partName, blobName,  '', 1, blobName",
    }, nullValues = { "null" })
    void shouldReturnBlobName_whenFolderNameIsNullOrEmptyAndPartSizeIsEqualToOne(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, blobName,  null, 2, partName",
            "partName, blobName,  '', 2, partName",
    }, nullValues = { "null" })
    void shouldReturnPartName_whenFolderNameIsNullOrEmptyAndPartSizeIsGraterThanOne(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, null, folderName, 1, folderName/partName",
            "partName, ''  , folderName, 1,  folderName/partName",

    }, nullValues = { "null" })
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsNullOrEmpty(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, null, folderName/, 1, folderName/partName",
            "partName, ''  , folderName/, 1, folderName/partName",
    }, nullValues = { "null" })
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsNullOrEmptyAndFolderNameEndsWithSlash(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, '',   '', 1, partName",
            "partName, null,   null, 1, partName",
            "partName, null,   '', 1, partName",
            "partName, '',   null, 1, partName",
    }, nullValues = { "null" })
    void shouldUsePartName_whenBothBlobNameAndFolderNameIsNullOrEmpty(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, blobName, folderName, 1, folderName/blobName",
    }, nullValues = { "null" })
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOne(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, blobName,  folderName/, 1,  folderName/blobName",
    }, nullValues = { "null" })
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOneAndFolderNameEndsWithSlash(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, blobName, folderName, 2, folderName/partName",
    }, nullValues = { "null" })
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOne(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "partName, blobName,  folderName/, 2,  folderName/partName",
    }, nullValues = { "null" })
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOneAndFolderNameEndsWithSlash(String partName, String blobName, String folderName, int partSize, String expected) {

        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, partSize)).isEqualTo(expected);
    }
}
