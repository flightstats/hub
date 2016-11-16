---
title: Channels
keywords: channel
last_updated: July 3, 2016
tags: [channel]
summary: "This section covers how to create, write to and read from channels."
sidebar: mydoc_sidebar
permalink: hub_channels_overview.html
folder: hub
---

## What is a channel?

In the Hub, a channel refers to a single named collection of data.  It is the primary method of organization of topics of data within the Hub.  

This section of the documentation covers how to interact with the channel API (creating and deleting channels; reading and writing to and from channels); 

## list channels

To obtain the list of channels:

`GET http://hub/channel`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://hub/channel/stumptown"
    }, {
      "name" : "ptown",
      "href" : "http://hub/channel/ptown"
    } ]
  }
}
```

## channel status

A GET on the status link for a channel will return the link to the latest item in the channel.
If a replicationSource is defined, it will also return the link the the latest in the replication hub.

`GET http://hub/channel/stumptown/status`

On success: `HTTP/1.1 200 OK`

```json
{
    "_links": {
        "self": {
            "href": "http://hub/channel/stumptown/status"
        },
        "latest": {
            "href": "http://hub/channel/stumptown/2015/01/23/17/33/08/895/1524"
        },
        "replicationSourceLatest": {
            "href": "http://hub-other/channel/stumptown/1526"
        }
    }
}
```

## channel limits {#limits}

`ttlDays` and `maxItems` are mutually exclusive.  When one field is set, the other must be 0 (zero).

`ttlDays` is used to limit the number of items in a channel by time.
The hub strictly enforces the time limit for range queries, and is less strict for indivdual items.

For example, if a channel has ttlDays = 3, and you query `/earliest/10', the results returned will be limited to 3 days ago from now.
Each item returned will be available for 15 minutes after the ttl has expired.

`maxItems` has a limit of 5000 items.  It is intended to be useful for channels that are updated infrequently.
Once a channel has reached its maxItems, older items will be removed.  maxItems should be considered eventually consistent.

For eaxmple, if you quickly insert 1,000 items into a channel with maxItems = 50, a query of `/latest/100` will return 100 items.
 Within 12 hours, the limit of 50 will be enforced.

If you change a channel from ttlDays to maxItems, it will also take up to 12 hours for the limit to be enforced.


## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://hub/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://hub/channel/stumptown`


