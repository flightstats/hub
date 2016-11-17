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


A historical channel is defined as a channel with the `historical` flag set to true (defaults to ```false```).

`historical: true`

This type of channel operates like a normal channel with data being added going forward in time. The exception being
you can control the exact time of each payload.

To insert data into a historical channel you specify the instant as part of the URI.

`POST http://hub/channel/stumptown/2016/5/14/12/00/00/000`

In order for the temporal features of the Hub to work (e.g. [webhooks](#webhook), [replication](#replication)) you must provide an additional
header when finalizing data for a given minute.

`minuteComplete: true` (defaults to `false`) DELETE http://hub/channel/stumptown
```


# How it works
A new channel is created with the mutability time set to "now". This is effectively the same as a normal channel creation. New data can be inserted linearly moving forward in time.

```
time -------------------|>
                       now
```

While the real-time inserts are happening a multithreaded writer is inserting and deleting items at various points in time prior to the mutability time.

```
               unstable |  stable  |
time -------------------|---------->
                                  now
```

Once you're happy with the historical data, the mutability time can be moved backwards, making the previously unstable data "stable".

```
             |       stable        |
time --------|--------------------->
                                  now
```

{% include links.html %}