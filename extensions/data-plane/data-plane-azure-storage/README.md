## Azure storage Data Plane module

### About this module

This module contains a Data Plane extension to copy data to and from Azure Blob storage.

When used as a source, it supports copying a single or multiple blobs.

The source `keyName` should reference a vault entry containing a storage [Shared Key](https://docs.microsoft.com/rest/api/storageservices/authorize-with-shared-key).

The destination `keyName` should reference a vault entry containing a JSON-serialized `AzureSasToken` object wrapping a [storage access signature](https://docs.microsoft.com/azure/storage/common/storage-sas-overview).

### AzureStorage DataAddress Configuration

The behavior of blobs transfers can be customized using DataAddress properties.

- When blobPrefix is present, transfer all blobs with names that start with the specified prefix.
- When blobPrefix is not present, transfer only the blob with a name matching the blobName property.
- Precedence: blobPrefix takes precedence over blobName when determining which objects to transfer. It allows for both multiple blobs transfers and fetching a single blob when necessary.

>Note: Using blobPrefix introduces an additional step to list all blobs whose name match the specified prefix.


An example source address:

- Single blob:
```json
{
  "dataAddress": {
    "properties": {
      "type": "AzureStorage",
      "container": "containerName",
      "account": "accountName",
      "blobName": "test/blob.bin",
      "keyName": "(see above)"
    }
  }
}
```
- Multiple blobs:
```json
{
  "dataAddress": {
    "properties": {
      "type": "AzureStorage",
      "container": "containerName",
      "account": "accountName",
      "blobPrefix": "test/",
      "keyName": "(see above)"
    }
  }
}
```
An example destination address:

- Single blob:
```json
{
    "dataDestination": {
        "properties": {
            "type": "AzureStorage",
            "container": "containerName",
            "account": "accountName",
            "folderName": "destinationFolder/",
            "blobName": "new-name",
            "keyName": "(see above)"
        }
    }
}
```

- Multiple blobs:
```json
{
    "dataDestination": {
        "properties": {
            "type": "AzureStorage",
            "container": "containerName",
            "account": "accountName",
            "folderName": "destinationFolder/",
            "keyName": "(see above)"
        }
    }
}
```
The `folderName` and the `blobName` are optional properties in destination address.


