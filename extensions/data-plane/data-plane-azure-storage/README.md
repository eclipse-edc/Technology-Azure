## Azure storage Data Plane module

### About this module

This module contains a Data Plane extension to copy data to and from Azure Blob storage.

When used as a source, it currently only supports copying a single blob.

The source `keyName` should reference a vault entry containing a storage [Shared Key](https://docs.microsoft.com/rest/api/storageservices/authorize-with-shared-key).

The destination `keyName` should reference a vault entry containing a JSON-serialized `AzureSasToken` object wrapping a [storage access signature](https://docs.microsoft.com/azure/storage/common/storage-sas-overview).

An example destination address:
```json
{
    "dataDestination": {
        "properties": {
            "type": "AzureStorage",
            "container": "containerName",
            "account": "accountName",
            "folderName": "test/",
            "blobName": "new-name",
            "keyName": "(see above)"
        }
    }
}
```

An example source address:
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
The `folderName` and the `blobName` are optional properties.


