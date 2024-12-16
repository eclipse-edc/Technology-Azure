# Azure Blob Core module

This extension provides a SDK to interact with a Blob Store.

The configuration values of this extension are listed below:

| Parameter name                        | Description                                        | Mandatory | Default value                      |
|:--------------------------------------|:---------------------------------------------------|:----------|:-----------------------------------|
| `edc.azure.block.size.mb`             | The block size, in mb, to parallel blob upload.    | false     | 4                                  |
| `edc.azure.max.concurrency`           | Maximum number of parallel requests in a transfer. | false     | 2                                  |
| `edc.azure.max.single.upload.size.mb` | Maximum size, in mb, for a single upload.          | false     | 60                                 |
| `edc.azure.token.expiry.time`         | Expiration time, in hours, for the SAS token.      | false     | 1                                  |
| `edc.blobstore.endpoint.template`     | Template for the blob service endpoint.            | false     | `https://%s.blob.core.windows.net` |

For detailed information,
check [the official documentation](https://learn.microsoft.com/en-us/java/api/com.azure.storage.blob.models.paralleltransferoptions?view=azure-java-stable).