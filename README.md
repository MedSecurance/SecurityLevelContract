# How to use
This context contains two folders, to show different aspects of the signature.

## RequirementsSelectorContainer
This folder contains the content required to serve a page (through Docker container) allowing the selection of requirements.

The web interface can be started by building the docker container and then executing it:

```
docker build -t contractpreparer .
docker run -p 4200:80 contractpreparer
```

There is alternatively an existing docker image under: `danielnaro/contractpreparer`.

This container requires the ontology server (`danielnaro/ontologyserver`) to be working, to populate the content of the page.

## MultipleXMLSignatures
Java projects which sign an XML file twice. The signatures are XADES compliant.

To sign a document named `to_sign.xml`, build and start the container (the file to sign must be mounted in the /data folder of the container):
```
docker build -t testsigning .
docker run -v MultipleXMLSignatures\DssCookbookTest\src\main\resources\forDockerData:/data testsigning
```

There is alternatively an existing docker image under: `danielnaro/testsigning`.
