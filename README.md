# MedSecurance 🚀

This repository contains an application to handle the contracts of MedSecurance.

## Key Features
- Creation of contract: allows the representation of the current agreement on the specification of the system, through the inclusion in one container of a set of documents. The set of considered documents is fixed and allows to:
  - prove the compliance with the standards considered in MedSecurance's Ontology (i.e. those documents which shall be provided to comply with the standards, can be included in the contract)
  - include inputs and outputs of the other MedSecurance's tools (thus allowing to check if the conclusions still hold, even with updated knowledge)
  
- Sign and validate the contract using the XAdES structure

- Overview of signed content:
  - when was the latest version of the document uploaded
  - at each organization, when was a contract with said version of the document signed and by whom.

## Installation

__IMPORTANT NOTE__: This tool has been tested on Windows with WSL2, and on linux.

### Prerequisites
1. Install [Docker](https://docs.docker.com/get-docker/).
2. Install [Docker Compose](https://docs.docker.com/compose/install/).

### Steps
1. Clone this repository:
   ```bash
   git clone <repository-url>
   ```
2. Modify the hardcoded DNS of the operating system to include:
   
| url | ip |
| --- | --- |
| signer.provider | IP of the deployment |
| signer.consumer | IP of the deployment |
| signer.requirements | IP of the deployment |
| signer.ontology | IP of the deployment |
| signer.authenticator | IP of the deployment |
| signer.minio | IP of the deployment |


4. Build and start the Docker containers:
   ```bash
   docker compose -f docker-compose.yml -p signinginterface up -d
   ```
   If some containers exit after one minute, restart them with:
   ```bash
   docker compose start
   ```

5. Access the provider point of view by acceding to `signer.provider` with the credentials `provider`/`provider`- Select the project you want to work on, or create new one by accessing `http://signer.provider/<project_name>`. Go to `Create contract`, upload the required files and reach the last step to download the contract.
6. Use `Sign contract` to access the form to sign the contract.
7. Access the consumer point of view by acceding to `signer.consumer` with the credentials `consumer`/`consumer`- Sign the contract received from the provider.
8. From the provider point of view, use the `verify signatures` button, to register the signatures from the consumer party.

## License
This project is licensed under the [MIT License](LICENSE).
