---
title: Install Locally
keywords: install
last_updated: July 3, 2016
tags: [install]
summary: 
sidebar: mydoc_sidebar
permalink: hub_install_locally.html
folder: hub
---

## Docker

Docker is the easiest way to run the hub on a single machine, and can also be used for development.

```
docker run -p 80:80 flightstats/hub:latest
```

To simplify deployment, this version of the hub does not use AWS services, and is not intended for long term storage.

### Docker Development

You can also use Docker to do development with the hub on you local machine.

```
bash docker/attackHubCodeWithGradle.sh
docker build docker
   Successfully built b69667db279b
docker run -p 80:80 b69667db279b
```

## More detailed instructions for active development

These instructions will allow you to run the hub without Docker.

### For OSX users:
* install [java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* install [homebrew](http://brew.sh/)
* install gradle 2.8 ```brew install homebrew/versions/gradle28```
* download the code ```git clone https://github.com/flightstats/hub.git```
* build ```gradle build```

### Using IntelliJ:
* Download and install [IntelliJ](https://www.jetbrains.com/idea/download/)
* Start IntelliJ
* Checkout from version control - https://github.com/flightstats/hub
* Import gradle project using gradle 1.8 using ```/usr/local/Cellar/gradle18/1.8/libexec```
* Install the lombok plugin
* Enable annotation processing
* Run SingleHubMain with VM Options :
```
-Xmx2g -Xms2g -Dapp.url=http://localhost:8080/
```
* install node - http://nodejs.org/download/
* cd src/test/integration/
* run install.sh
* create integration_config_local.js with 
```
hubDomain='localhost:9080';
integrationTestPath =  '';
```
* jasmine-node --captureExceptions --forceexit .

### Using DynamoDB Local

* [Download](http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html )
* Install it somewhere
* run ```java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -inMemory```
* create src/main/resources/default_local.properties with ```dynamo.endpoint=localhost:8000```

### Gradle

To run gradle tests locally, run this command from the hub root directory
```
gradle --debug clean test
```

You will need to have [AWS credentials](http://docs.aws.amazon.com/general/latest/gr/managing-aws-access-keys.html) installed:

* create src/main/resources/default_local.properties with ```aws.credentials=/path/to/hub_test_credentials.properties```


{% include links.html %}