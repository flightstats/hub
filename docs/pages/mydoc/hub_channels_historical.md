---
title: Historical Channels
keywords: channel, historical
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_historical.html
folder: hub
---

## Goals
* Reuse the Hub's temporal APIs for data created in the past.
* Preserve the immutability and ordering contracts for stable data.
* Allow multithreaded writes of historical data.
* Provide a way to unwind changes or delete historical data that isn't stable yet.
* Provide a mechanism to verify historical data before it is made stable.

## Proposed Change
* A channel can optionally have a `mutableTime` attribute, which is a valid time in the past.
    * Once mutableTime is set, items can be inserted into the channel before or equal to the mutableTime.
    * Channels with mutableTime can not have `ttlDays` and `maxItems`, as mutableTime channels are intended for long term storage. 
* Normal Hub channel rules and expectations apply to the stable portion of the channel (e.g. real time inserts, webhooks, replication, etc.)
* Items can be inserted or deleted in any order before the mutableTime, allowing for multithreading.
* Once the changes are final you can move the mutableTime earlier in time, making all items after the new mutableTime "stable".
* Mutable items can be queried using the `epoch` query parameter.
* Existing channels can be converted to this type by someone with admin access.
* This would replace the current notion of "Historical Channels" with the `historical` flag.

## How this all works

### Creation
A new channel is created with the mutableTime set to a time in the past.  Like all channels, real time data can be inserted.  

```
                  mutableTime   now
time -------------------|---------->
```

### Modifying Data
While the real-time inserts (*) are happening a multithreaded writer is inserting (+) and deleting (-) items at various points in time prior to mutableTime.

```
                  mutableTime    
               mutable  | immutable  
time -------------------|------------>
          +      - + - +             *
```

### Querying Data

To check the mutable items, all query endpoints (time, next, previous, latest, earliest) support an optional query paramater, `epoch`
The epoch defaults to `IMMUTABLE`.  

```
       <-              ALL          ->    
       <-      MUTABLE  | IMMUTABLE ->
time -------------------|------------>
```


### Changing mutableTime

Once you're happy with the historical data, the mutableTime can be moved backwards, making the previously unstable data "stable".

```
           mutableTime    
       unstable |          stable  
time -----------|-------------------->
       - + + -                       *
```

{% include links.html %}