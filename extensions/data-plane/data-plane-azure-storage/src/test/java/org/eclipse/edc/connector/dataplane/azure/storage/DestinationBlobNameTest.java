/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Marco Pirmo (BMW AG)
 *
 */

package org.eclipse.edc.connector.dataplane.azure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class DestinationBlobNameTest {

    @Test
    void shouldReturnBlobName_whenFolderNameIsEmptyAndPartSizeIsEqualToOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "";
        var expected = "blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldReturnBlobName_whenFolderNameIsNullAndPartSizeIsEqualToOne() {

        var partName = "partName";
        var blobName = "blobName";
        var expected = "blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, null);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldReturnPartName_whenFolderNameIsEmptyAndPartSizeIsGraterThanOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "";
        var expected = "partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldReturnPartName_whenFolderNameIsNullAndPartSizeIsGraterThanOne() {

        var partName = "partName";
        var blobName = "blobName";
        var expected = "partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, null);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsEmpty() {

        var partName = "partName";
        var blobName = "";
        var folderName = "folderName";
        var expected = "folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsNull() {

        var partName = "partName";
        var folderName = "folderName";
        var expected = "folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(null, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsNullAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var folderName = "folderName/";
        var expected = "folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(null, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }


    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsEmptyAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var blobName = "";
        var folderName = "folderName/";
        var expected = "folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
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

    @Test
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName";
        var expected = "folderName/blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOneAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName/";
        var expected = "folderName/blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOne() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName";
        var expected = "folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOneAndFolderNameEndsWithSlash() {

        var partName = "partName";
        var blobName = "blobName";
        var folderName = "folderName";
        var expected = "folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }
}
