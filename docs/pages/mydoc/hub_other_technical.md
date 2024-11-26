---
title: Technical Details
keywords: 
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_other_technical.html
folder: hub
---


## Overview

The Hub is designed as a fault tolerant, highly available service for data distribution and storage.

It uses a in-process short term cache, called Spoke, to provide read after write consistency and low latency performance.

For the sake of simplicity, all Spoke operations assume "all data is everywhere".  This allows us to ignore the complexity of hash rings for sharding data.
While we recommend a minimum cluster of 3 servers for fault tolerance, the hub can run with just a single server.

Long term storage is provided by [S3](http://aws.amazon.com/s3/).  The modular design allows the addition of new datastores.

## Time

A key for an item inserted into the hub is the combination of the current system time in UTC and a hash to break millisecond ties, {year}/{month}/{day}/{hour}/{minute}/{second}/{hash}
To provide consistent ordering for [sequential writes](hub_other_use_cases.html#sequential), the system time between the instances needs to be [coordinated](hub_other_ntp.html).

## Writes

Items written to the hub are immediately compressed and then assigned a key.  Each item is concurrently written to all Spoke instances.  A successful Spoke write is defined as at least 50% of the nodes.

After a successful Spoke write, the item is put on an internal bounded queue to write to S3. A background process is used to verify that all items actually exist in S3.

## Reads

Read requests attempt to read from Spoke first, if the item is within the Spoke TTL (defaults to 60 minutes).
The hub attempts each Spoke nodes in random order until the item is found.  If not found or outside the Spoke TTL, S3 is queried.

## Querying

Query requests are latest, earliest, next, previous and time ranges.
If a query is determined to be entirely within Spoke's TTL, all Spoke nodes are queried concurrently.
Otherwise, S3 is also queried concurrently.
The union of all items found are then filtered based on the request criteria.

## Callbacks

[Webhook](hub_notifications_webhooks.html), [Websocket](hub_notifications_websocket.html), [Events](hub_notifications_events.html), 
and [Replication](hub_channels_replication.html) are built on the query interface, 
and presume that items are written within 'app.stable_seconds' (default 5 seconds) of the key being assigned.
All of our deployments to EC2 have been able to meet this timing requirement easily.

## Monitoring

http://hub/health will show if a specific hub host is up, and what version it is running.

The hub uses [New Relic](http://newrelic.com/) for API level monitoring.  Since all hub and spoke interactions are http, the transactions automatically show up in New Relic.

The hub also publishes detailed metrics data to [Hosted Graphite](http://hostedgraphite.com/) and [Grafana](https://lnrscirium.grafana.net/)
Both are more useful for detailed metrics per channel and the health of the system.

## storage

The Hub has two options to store data:
* It can use a combination of a local cache and [S3](https://aws.amazon.com/s3/)
* It can use a single drive shared across the cluster
 
For Hubs which use S3, the channel option `storage` can make a significant difference in costs.
High volume channels should prefer `BATCH` to reduce costs.


## access control

If admins set hub property `hub.protect.channels` to `true`, normal users of the system will not be able to change a 
channel in a way that could cause data loss.   
If `hub.protect.channels` is `false`, end users can optionally set `protect` on specific channels.

If `protect` is true:
* `storage` can only be changed to `BOTH`
* `tags` can not be removed
* `maxItems` and `ttlDays` can not decrease
* `replicationSource` can not change
* `protect` can not be reset from `true`
* channel can not be deleted

Instead, a user will need to make the command(s) while logged into a hub server.
 
```
curl -i -X PUT --header "Content-type: application/json"  --data '{"ttlDays" : 1}' http://localhost:8080/channel/stumptown
```

## encrypted-hub

The Encrypted Hub (EH) is a separate installation of The Hub.
EH also has some additional features to the normal Hub:

* All channel items are encrypted at rest (this relies on disk level encryption)
* All channel items are encrypted in flight
* All access to channel items (reads and writes) require authentication and are access controlled

Channel inserts can be audited by a GET or HEAD to each channel item.  The creator of the record is returned in a `User` header.

## internal api

The hub uses a number of endpoints not intended for use by clients.
All of these are under http://hub/internal
Some of these are useful for debugging, such as :
* Read only view into zookeeper values - http://hub/internal/zookeeper
* Traces of calls into the hub, including active, slowest and recent - http://hub/internal/traces

## consistency

* All times from the Hub are in UTC.
* By default all iteration, queries, webhooks and websockets return items with stable ordering.  Data is considered stable when iteration will provide consistent results.
* All requests for a specific item by id will return that item if it exists.


## development

The Hub is a work in progress.  If you'd like to contribute, let us know.

[Install locally](hub_install_locally.html)

General Rules for Development:
* Only pull from master
* Create a new branch for features and bugs, avoiding '/' in the branch name
* after testing, create a pull request from the feature branch to master




{% include links.html %}