## Azure storage Data Plane module

### About this module

This module contains a Data Plane extension to copy data to and from Azure Blob storage.

When used as a source, it supports copying a single or multiple blobs.

The source `keyName` should reference a vault entry containing a
storage [Shared Key](https://docs.microsoft.com/rest/api/storageservices/authorize-with-shared-key).

The destination `keyName` should reference a vault entry containing a JSON-serialized `AzureSasToken` object wrapping
a [storage access signature](https://docs.microsoft.com/azure/storage/common/storage-sas-overview).

### AzureStorage DataAddress Configuration

The behavior of blobs transfers can be customized using DataAddress properties.

- When blobPrefix is present, transfer all blobs with names that start with the specified prefix.
- When blobPrefix is not present, transfer only the blob with a name matching the blobName property.
- Precedence: blobPrefix takes precedence over blobName when determining which objects to transfer. It allows for both
  multiple blobs transfers and fetching a single blob when necessary.

> Note: Using blobPrefix introduces an additional step to list all blobs whose name match the specified prefix.


An example source address:

- Single blob:

```json
{
  "dataAddress": {
    "type": "AzureStorage",
    "container": "containerName",
    "account": "accountName",
    "blobName": "test/blob.bin",
    "keyName": "(see above)"
  }
}
```

- Multiple blobs:

```json
{
  "dataAddress": {
    "type": "AzureStorage",
    "container": "containerName",
    "account": "accountName",
    "blobPrefix": "test/",
    "keyName": "(see above)"
  }
}
```

An example destination address:

- Single blob:

```json
{
  "dataDestination": {
    "type": "AzureStorage",
    "container": "containerName",
    "account": "accountName",
    "folderName": "destinationFolder/",
    "blobName": "new-name",
    "keyName": "(see above)"
  }
}
```

- Multiple blobs:

```json
{
  "dataDestination": {
    "type": "AzureStorage",
    "container": "containerName",
    "account": "accountName",
    "folderName": "destinationFolder/",
    "keyName": "(see above)"
  }
}
```

The `folderName` and the `blobName` are optional properties in destination address.

### Transfer Provisioning

When provisioning a transfer, the key with the value of `account` will be resolved from the vault.
This key will be used to authenticate with Azure. If such a key is not present environment/system variables will be used.

### AzureStorage Transfer Configuration

The existing implementation takes under consideration transfer of files up to 200GB and that can be accomplished within
the space of one hour. If your usage surpasses these limits or any other kind of additional tuning is needed,
please [check this documentation](../../common/azure/azure-blob-core/src/main/java/org/eclipse/edc/azure/blob/BlobStorageConfiguration.java)
containing the transfer configurations.

#### File larger than 200GB

To ease transfer of a large file, it is divided in several blocks. The default block size is 4MB which limits the
maximum transfer file size to 200GB. This is due to Azure SDK limiting the maximum number of blocks in a transfer to
50000, resulting in aforementioned maximum of 200GB. So, if there is a need to upload
files with larger sizes, the `edc.azure.block.size.mb` property must be updated.

#### Token expires before transfer is completed

The default value of the SAS Token expiration if one hour after its creation. For larger files and/or due to network
limitation the transfer can take longer than one hour.
In this situation please update the `edc.azure.token.expiry.time` to a higher value.
