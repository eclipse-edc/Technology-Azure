/*
 *  Copyright (c) 2024 BMW Corporation
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class DestinationBlobNameTest {


    @Test
    void shouldReturnBlobName_whenFolderNameIsEmptyAndPartSizeIsEqualToOne() {

        String partName="partName", blobName = "blobName",folderName="",expected="blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }
    @Test
    void shouldReturnBlobName_whenFolderNameIsNullAndPartSizeIsEqualToOne() {

        String partName="partName", blobName = "blobName",folderName=null,expected="blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldReturnPartName_whenFolderNameIsEmptyAndPartSizeIsGraterThanOne() {

        String partName="partName", blobName = "blobName",folderName="",expected="partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldReturnPartName_whenFolderNameIsNullAndPartSizeIsGraterThanOne() {

        String partName="partName", blobName = "blobName",folderName=null,expected="partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsEmpty() {

        String partName="partName", blobName = "",folderName="folderName",expected="folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsNull() {

        String partName="partName", blobName = null,folderName="folderName",expected="folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsNullAndFolderNameEndsWithSlash() {

        String partName="partName", blobName = null,folderName="folderName/",expected="folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }


    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenBlobNameIsEmptyAndFolderNameEndsWithSlash() {

        String partName="partName", blobName = "",folderName="folderName/",expected="folderName/partName";
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

        String partName = "partName", blobName="blobName", folderName="folderName", expected="folderName/blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithBlobNameUsingSlash_whenPartSizeEqualsOneAndFolderNameEndsWithSlash() {

        String partName = "partName", blobName="blobName", folderName="folderName/", expected="folderName/blobName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 1)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOne() {

        String partName = "partName", blobName="blobName", folderName="folderName", expected="folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }

    @Test
    void shouldAppendFolderNameWithPartNameUsingSlash_whenPartSizeIsGraterThanOneAndFolderNameEndsWithSlash() {

        String partName = "partName", blobName="blobName", folderName="folderName", expected="folderName/partName";
        DestinationBlobName destinationBlobName = new DestinationBlobName(blobName, folderName);
        assertThat(destinationBlobName.resolve(partName, 2)).isEqualTo(expected);
    }
}
