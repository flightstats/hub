---
title: Global Channels
keywords: channel, global
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_global.html
folder: hub
---

## Global Hub goals
* Simplify global distribution of channel data to geographically distant Hub Clusters.
* Support a single originating source of data per channel
* Maintain the same keys for data between clusters
* Read after Write Guarantee
* Interface compatible with Geo DNS

## Overview

To support Global channels, we are making an addition to the [Channel API](hub_channels_creating.html)
A Global channel is defined as a channel with the `global` object set.

```
    global : {
        master : "http://hub.europe",
        satellites : [ "http://hub.america", "http://hub.asia" ]
    }

```

* `master` should be set to the load balanced url of the hub cluster which should receive writes.
* `satellites` should be set to the load balanced url(s) of the hub cluster which should receive data from the master.
* The channel configuration will be the same on all hub clusters.
* A global channel will automatically receive a tag of "global".

### Writes

{% include image.html file="GlobalWrite.png" url="" alt="" caption="" %}

Writes received by the Master are handled in the same way as existing Hub clusters.
The data is written to Spoke and then eventually written to S3.  Data is removed from Spoke eventually (default 1 hour).

Satellites will receive their data via a SECOND group callback, typically 1-2 seconds behind the Master.
The data is only written to Spoke.

Writes received by the Satellites will be internally redirected to the Master.

### Reads

{% include image.html file="GlobalRead.png" url="" alt="" caption="" %}

Masters will read items in the current way, using Spoke as a cache, and then attempting S3.

Satellites will read from Spoke for items within the TTL window, and then fall back to the Master.
To provide a "Read after Write Guarantee", if an item is requested from a Satellite for a time after the latest item from the Master, the Satellite will read from the Master first.

### Queries

{% include image.html file="GlobalQuery.png" url="" alt="" caption="" %}

Master queries will work in the current fashion.  If a query is entirely within the Spoke cache, it will only query Spoke.
Otherwise, queries go to both Spoke and S3.

Satellites queries will work in a similar fashion.  If a query is entirely within the Spoke cache, it will only query Spoke.
Otherwise, queries go to both Spoke and the Master.

## Comparison with Replication

* Global channels should be at the same environment level - dev, staging, prod
* Replication recreates all the data in a different S3 bucket, Global does not
* Global can use multiple Hub clusters to answer questions, Replication does not
* Global can support failover, and the use of a global hub domain


{% include links.html %}