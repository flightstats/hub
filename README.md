Read the docs: https://flightstats.github.io/hub/

![hub logo](./docs/images/HubLogoSmall.png)

# What is the Hub? 

The hub is a distributed linked list.   
The hub is a messaging system.   
The hub is a long term data store.   

## The hub is like a Key Value Store 
* Stores each item at a unique key (url) (http://hub/channel/whatIs/2016/12/31/23/59/59/bhJ23I)
* Items are available for an arbitrarily long time
* Fault tolerant, easy to cluster
* Requires a quorum for a successful write 

### with some differences 
* Most KV stores allow mutations.  The hub does not.
* The hub imposes that all item keys always move forward in time
* The hub offers ordering guarantees to provide consistent answers to time based queries
 
## The hub is like a Messaging system
* Items are immutable
* Item keys always increase

### and some differences
* Most messaging systems do not let you access arbitrarily old items
* Many messaging systems are difficult to cluster
* Many messaging system require custom clients


### How to build hub:

Run ./gradlew build

The above command compiles the code, runs the unit tests and integration tests.~~~~

### How to run System tests:

From Local:

1) Setup hub:
    a) ./localDocker.sh (from root directory)
    b) docker run -p 80:80 hub:local
   
2) set base.url property in intergation-hub.properties
    
3) Go to root of hub project source and run below command
   ./gradlew systemTest -i    
   
From Jenkins:

1) Goto https://ddt-jenkins.pdx.prod.flightstats.io/job/hub-system-test/ 
2) Click on "Build With Parameters" 
3) Add your git branch and start the build.