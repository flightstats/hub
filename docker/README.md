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
in /etc/hub on the Docker 'host' and mounting that directory with the ``--volume`` command-line options.
Alternately, there is an example docker-compose.yml file that describes the docker setup for using
the hub as part of a cluster, and the host volumes you may want to bind-mount.

For example, the default output location for logs is /mnt/log. One might wish to have a log volume mounted at
/mnt/log or /var/log/hub, and use that volume bind-mounted into the container for persistent logs on the host.

Spoke: (/mnt/spoke)

Unless a persistent volume of some kind is used with the container, the contents of "spoke" will
vanish into the ether when the container is stopped. The default location of the spoke path is /mnt/spoke,
which can be bind-mounted into the container with ``docker run --volume /spoke:/mnt/spoke ...``, for example.
This is also demonstrated in the example docker-compose.yml file in this dir.

## Jvm

We use a runfile to set some JVM memory settings and help manage things.
The Dockerfile includes default environment variables that supply the JVM memory settings to the run file.

There are three: MIN_HEAP, MAX_HEAP, and NEW_SIZE.

These can be specified using the -e or --env flag with ``docker run``, e.g.

``docker run -p 80:80 -e MAX_HEAP=4g -e MIN_HEAP=2g -e NEW_SIZE=100m flightstats/hub``

The clustered version generally uses higher memory settings - e.g. 1g min and 2g max with a 100m min new size,
but keep in mind it also uses a significant amount of the operating system's file cache, which means you don't want to dedicate too
much of the system RAM for java.
