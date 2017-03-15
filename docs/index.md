---
title: Getting Started with Flightstats/Hub
keywords: homepage
tags: [getting_started]
sidebar: mydoc_sidebar
permalink: index.html
summary: These brief instructions will help you get started quickly Flightstats/Hub.
---

{% include image.html file="HubLogoSmall.png" url="" alt="" caption="Hub" %}


## overview

The Hub is a fault tolerant, highly available HTTP API for data distribution and storage.  

Data
Channels contain uniquely addressable items that are iterable and query-able by time.  Each item may be up to to 40 MB.   
We use the Hub for sharing real time data between teams.  

You can read more about what the hub in the wiki   
* [What is the Hub?](hub_overview_whatisthehub.html)
* [Goals](hub_overview_goals.html)

## quick start

Install Docker and use the hub docker image at https://hub.docker.com/r/flightstats/hub/

```
docker run -p 80:80 flightstats/hub:latest
```

To update your existing hub docker image:

```
docker pull flightstats/hub
```


## health check

The Health Check returns a 200 status code when the server can connect to each data store.
If the server can not access a data store, it will return a 500 status code.

Responding to http connections is the last step on startup, so the health check will be unresponsive until startup is complete.
On shutdown, the server immediately stops responding to new http connections, so there are no separate codes for startup and shutdown.

`GET http://hub/health`

```json
{
  "healthy" : true,
  "description" : "OK",
  "version" : "2014-03-26.126"
}
```

## hub resources

To explore the Resources available in the Hub, go to http://hub/

**Note**
For the purposes of this document, the Hub is at http://hub/.
On your local machine it is at: http://localhost/ (docker) or http://localhost:9080/ (native)


## error handling

Clients should consider handling transient server errors (500 level return codes) with retry logic.  This helps to ensure that transient issues (networking, etc)
  do not prevent the client from entering data. For Java clients, this framework provides many options - https://github.com/rholder/guava-retrying
We also recommend clients use exponential backoff for retries.



{% include links.html %}
