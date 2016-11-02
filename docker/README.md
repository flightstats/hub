Docker materials
===================

* [Intro](#intro)
* [Configuration](#configuration)
* [Jvm](#jvm)

## Intro

These are the files for building a docker-image version of the hub.
Alone, it will run as a disk-based, self-contained version (com.flightstats.hub.app.singleHubMain), mostly useful
for testing and quick mock-ups.

## Configuration

Various configuration options can change the application's behavior.

Logback.xml and hub.properties: (/etc/hub)

The hub uses logback to manage its logfiles. A default logback.xml config file
is located in /etc/hub/logback.xml in the container.

Hub.properties is optional and omitted by default unless the hub is running in a cluster mode, in which case configuration
parameters are required. Then hub.properties
supplies necessary configuration like s3 bucket name, dynamo config, and where to find the coordinating zookeeper nodes.

To modify the logging configuration, and/or to supply configuration for a clustered hub installation,
logback.xml and hub.properties can be optionally supplied to the container by putting them
in /etc/hub on the Docker 'host' and mounting that directory with the --volume command-line options.
Alternately, there is an example docker-compose.yml file that describes the docker setup for using
the hub as part of a cluster, and the host volumes you may want to bind-mount.

For example, the default output location for logs is /mnt/log. One might wish to have a log volume mounted at
/mnt/log, and use that volume bind-mounted into the container for persistent logs on the host.

Spoke: (/mnt/spoke)

Unless a persistent volume of some kind is used with the container, the contents of "spoke" will
vanish into the ether when the container is stopped. The default location of the spoke path is /mnt/spoke,
which can be bind-mounted into the container with docker run --volume /mnt/spoke:/mnt/spoke ..., for example.
This is also demonstrated in the example docker-compose.yml file in this dir.

## Jvm

We use a runfile to set some JVM memory settings and determine what mode the hub will run in.
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
