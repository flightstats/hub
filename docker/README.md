Docker materials
===================

* [Intro](#intro)
* [installations](#installations)

## Intro

These are the files for building a docker-image version of the hub.
Alone, it will run as a disk-based, self-contained version (com.flightstats.hub.app.singleHubMain), mostly useful
for testing a quick mock-ups.

Various configuration options can change the application's behavior:

Logback.xml is optional, but a default file is put in /etc/hub/logback.xml and provides logging configuration.

Hub.properties is also optional and omitted by default unless the hub is running in a cluster mode. Then hub.properties
supplies necessary configuration like s3 bucket name, dynamo config, and where to find the coordinating zookeeper nodes.

Right now, we use a hack in the runfile to set some JVM memory settings and determine what mode the hub will run in.
The override-able CMD in the dockerfile
is comprised of the following five variables: App Name, Java Class, Min Heap, Max Heap, and Min New Size.

```
"hub", "com.flightstats.hub.app.SingleHubMain", "256m", "512m", "10m"
```

"com.flightstats.hub.app.SingleHubMain" is the single, local version of the hub and "com.flightstats.hub.app.HubMain" is the 
clustered version. 

The clustered version generally uses higher memory settings - e.g. 1g min and 2g max with a 100m min new size,
but keep in mind it also uses a significant amount of the operating system's file cache, which means you don't want to dedicate too
much of the system RAM for java.

