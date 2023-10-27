# Azure Key Vault Extension

The extension provides a `Vault` implementation interfacing with an Azure Key Vault.

## Authentication

This extension connects to Azure Key Vault using the
standard `AzureDefaultCredential`
provided by the Azure Identity library. This generic credential fits most use-cases and will attempt to authenticate via
a predefined chain of methods until one is successful. More details about the authentication methods used can be found
in  
this [page]([DefaultAzureCredential](https://learn.microsoft.com/en-gb/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable)).

### Example 1: connect with Principal client id and a client secret (see [EnvironmentCredential](https://learn.microsoft.com/en-gb/java/api/com.azure.identity.environmentcredential?view=azure-java-stable))

The following environments variables must be set:

- `AZURE_CLIENT_ID`
- `AZURE_CLIENT_SECRET`
- `AZURE_TENANT_ID`
-

### Example 2: connect with Principal client id and a client certificate (see [EnvironmentCredential](https://learn.microsoft.com/en-gb/java/api/com.azure.identity.environmentcredential?view=azure-java-stable))

The following environments variables must be set:

- `AZURE_CLIENT_ID`
- `AZURE_CLIENT_CERTIFICATE_PATH`
- `AZURE_CLIENT_CERTIFICATE_PASSWORD`
- `AZURE_TENANT_ID`




