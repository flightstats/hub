---
title: Use Cases
keywords: 
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_other_best_practices.html
folder: hub
---

## Channel Storage Guidelines

tl;dr
If you are inserting more than 10 items per minute, use BATCH channels and bulk endpoints for best performance and lowest cost.

### Bulk vs BATCH

Bulk means the Bulk API, posting to /channel/<name>/bulk and getting items back out with the query parameter ?bulk=true.

BATCH is the storage mechanism the hub uses to write items to S3.  The user sets the storage of the channel to SINGLE or BATCH.  

### What's the difference between SINGLE and BATCH?

The default storage setting is SINGLE.  This means that the hub writes every item to Spoke, then asynchronously writes the item to AWS S3.
After 15 minutes, the hub verifies that all of the items in Spoke are also in S3.
  
BATCH storage creates a [MINUTE Webhook](https://flightstats.github.io/hub/hub_notifications_webhooks.html) 
Every minute the webhook triggers, and the hub streams an entire minutes worth of data into S3, writing two items, the compressed content and an index.

Therefore, a BATCH channel costs only 2 S3 writes per minute.

### Low Volume (less than 10 items per minute)

The single item POST and GET are great for browsing a channel in a web browser.  They are simple to use with any web client.
There is very little cost or performance to optimize in this scenario.

### High Volume (up to 200+ items per second)

Once a channel gets 10 items per minute, it makes financial sense to use a channel with BATCH storage.
At this volume, clients must use the [Bulk API](https://flightstats.github.io/hub/hub_channels_insert.html#bulk) for reading data outside the Spoke cache. 
The Bulk endpoints can also greatly reduce the number of calls needed for reading data within the Spoke cache. 


{% include links.html %}