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

## Features
* A channel can optionally have a `mutableTime` attribute, which is a valid time in the past.
    * Once mutableTime is set, items can be inserted into the channel before or equal to the mutableTime.
    * Channels with mutableTime can not have `ttlDays` and `maxItems`, as mutableTime channels are intended for long term storage. 
* Normal Hub channel rules and expectations apply to the stable portion of the channel (e.g. real time inserts, webhooks, replication, etc.)
* Items can be inserted or deleted in any order before the mutableTime, allowing for multithreading.
* Once the changes are final you can move the mutableTime earlier in time, making all items after the new mutableTime "stable".
* Mutable items can be queried using the `epoch` query parameter.
* Existing channels can be converted to have a mutableTime, as long as the storage type is SINGLE.
* This replaces the current notion of "Historical Channels" with the `historical` flag.

## How this all works

### Creation
A new channel is created with the mutableTime set to a time in the past.  Like all channels, real time data can be inserted.  

```
                  mutableTime   now
time -------------------|---------->
```

**HTTPie Example**
```bash
-> % http POST localhost:8080/channel/aTestChannel/2017/08/17/09/00/00/000 \
exampleValue:=3 exampleText:='"three"'
```


### Modifying Data
While the real-time inserts (*) are happening a multithreaded writer is inserting (+) and deleting (-) items at various points in time prior to mutableTime.

```
                  mutableTime    
               mutable  | immutable  
time -------------------|------------>
          +      - + - +             *
```


**HTTPie Example:  - letting hub create the hash**
```bash
http POST localhost:8080/channel/aTestChannel/2017/08/17/09/00/00 \
exampleValue:=3 exampleText:='"three"'
```

**and user defined hash**
```bash
-> % http POST localhost:8080/channel/aTestChannel/2017/08/17/09/00/00/000/qwerty \
exampleValue:=3 exampleText:='"three"'
```


### Querying Data

To check the mutable items, all query endpoints (time, next, previous, latest, earliest) support an optional query paramater, `epoch`
The epoch defaults to `IMMUTABLE`.  

```
       <-              ALL          ->    
       <-      MUTABLE  | IMMUTABLE ->
time -------------------|------------>
```

***HTTPie example: will return the links from the above two inserts***
```bash
-> % http localhost:8080/channel/aTestChannel/2017/08/17?epoch=MUTABLE
```

### Changing mutableTime

Once you're happy with the historical data, the mutableTime can be moved backwards, making the previously unstable data "stable".

```
           mutableTime    
       unstable |          stable  
time -----------|-------------------->
       - + + -                       *
```

## singleHub v clustered hub

Historical inserts are supported in both singleHub and the clustered hub.

The clustered hub writes real-time items to Spoke, then writes them asynchronously into S3.
Historical items are handled differently, in that they are written directly into S3, bypassing Spoke.
This should prevent large historical data volumes from impacting Spoke's performance.

The singleHub writes all items to the file system, and historical items are no different than real-time items.

{% include links.html %}