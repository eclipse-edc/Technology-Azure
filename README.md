# EDC Technology Azure

[![documentation](https://img.shields.io/badge/documentation-8A2BE2?style=flat-square)](https://eclipse-edc.github.io)
[![discord](https://img.shields.io/badge/discord-chat-brightgreen.svg?style=flat-square&logo=discord)](https://discord.gg/n4sD9qtjMQ)
[![latest version](https://img.shields.io/maven-central/v/org.eclipse.edc.azure/azure-test?logo=apache-maven&style=flat-square&label=latest%20version)](https://search.maven.org/artifact/org.eclipse.edc.azure/azure-test)
[![license](https://img.shields.io/github/license/eclipse-edc/Technology-Azure?style=flat-square&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
<br>
[![build](https://img.shields.io/github/actions/workflow/status/eclipse-edc/Technology-Azure/verify.yaml?branch=main&logo=GitHub&style=flat-square&label=ci)](https://github.com/eclipse-edc/Technology-Azure/actions/workflows/verify.yaml?query=branch%3Amain)
[![snapshot build](https://img.shields.io/github/actions/workflow/status/eclipse-edc/Technology-Azure/trigger_snapshot.yml?branch=main&logo=GitHub&style=flat-square&label=snapshot-build)](https://github.com/eclipse-edc/Technology-Azure/actions/workflows/trigger_snapshot.yml)
[![nightly build](https://img.shields.io/github/actions/workflow/status/eclipse-edc/Technology-Azure/nightly.yml?branch=main&logo=GitHub&style=flat-square&label=nightly-build)](https://github.com/eclipse-edc/Technology-Azure/actions/workflows/nightly.yml)

---

This repository contains Azure-specific implementations for several SPIs of the [Eclipse Dataspace Components Connector](https://github.com/eclipse-edc/Connector):
- various storage backends based on CosmosDB
- `KeyVault` based on Azure KeyVault
- data transfer facilities based on Azure BlobStore

## Documentation

Base documentation can be found on the [documentation website](https://eclipse-edc.github.io).
Developer documentation can be found under [docs/developer](docs/developer), \
where the main concepts and decisions are captured as [decision records](docs/developer/decision-records).

## Contributing

See [how to contribute](https://github.com/eclipse-edc/eclipse-edc.github.io/blob/main/CONTRIBUTING.md).
