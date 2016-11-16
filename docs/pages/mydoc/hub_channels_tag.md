---
title: Channel Tag Interface
keywords: channel
last_updated: July 3, 2016
tags: [channel tag]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_tag.html
folder: hub
---


Tags are used to logically group channels.  


# List Tags {#list}

To retrieve all of the tags in the Hub:

`GET http://hub/tag`

On success: `HTTP/1.1 200 OK`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/tag"
    },
    "tags" : [ {
      "name" : "orders",
      "href" : "http://hub/tag/orders"
    }, {
      "name" : "coffee",
      "href" : "http://hub/tag/coffee"
    } ]
  }
}
```

Any of the returned tag links can be followed to see all of the channels with that tag:

`GET http://hub/tag/coffee`

On success: `HTTP/1.1 200 OK`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/tag/coffee"
    },
    "latest": {
        "href": "http://hub/tag/coffee/latest"
    },
    "earliest": {
        "href": "http://hub/tag/coffee/earliest"
    },
    "time": {
        "href": "http://hub/tag/coffee/time"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://hub/channel/stumptown"
    }, {
      "name" : "spella",
      "href" : "http://hub/channel/spella"
    } ]
  }
}
```

# tag unions {#union}

Tags can also be used for a read-only union set of all it's channels.
[next and previous links](hub_channels_reading.html#next-and-previous), [latest](hub_channels_reading.html#latest),
[earliest](hub_channels_reading.html#earliest) and [time](hub_channels_time.html) work the same as their channel analogs.
Tag operations can be accessed through /tag/{tag-name} or /channel/{channel-name}/

Example operations:

```
GET http://hub/tag/coffee/latest
GET http://hub/tag/coffee/earliest
GET http://hub/tag/coffee/2015/06/25/16
GET http://hub/tag/coffee/2015/06/24/19/48/17/000/abc/previous
GET http://hub/tag/coffee/2015/06/24/19/48/17/000/abc/next/10
GET http://hub/channel/spella/2015/06/24/19/48/17/000/abc/next/10?tag=coffee
```

Operations through the channel interface (/channel/{channel-name}/) can a tag parameter.  Using the tag parameter allows the user to stay in the tag context.

For example:

`GET http://hub/tag/coffee/latest`

returns a redirect to

`http://hub/channel/spella/2015/06/24/19/48/17/000/abc?tag=coffee`

Following the previous on that item and including the tag

`http://hub/channel/spella/2015/06/24/19/48/17/000/abc/previous?tag=coffee`

returns a redirect to an item on a different channel

`http://hub/channel/stumptown/2015/06/24/19/40/17/000/qwe?tag=coffee`


{% include links.html %}