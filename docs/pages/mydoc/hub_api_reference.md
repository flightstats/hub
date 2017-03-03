---
title: REST API Reference
keywords: API
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_api_reference.html
folder: hub
---

# Channels

## [Create Channel](hub_channels_creating.html)

`PUT http://{hub}/channel/{channel-name}`

Content-type: application/json

```json
{
   "ttlDays": "14",
   "description": "a sequence of all the coffee orders from stumptown",
   "tags": ["coffee"]
}
```


## [Delete Channel](hub_channels_delete.html)

`DELETE http://{hub}/channel/{channel-name}`

## Write

### [Insert Individual Payload](hub_channels_insert.html#individual)

```
POST http://{hub}/channel/{channel-name}
Content-type: text/plain
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

### [Insert Bulk](hub_channels_insert.html#bulk)

```
POST http://{hub}/channel/{channel-name}/bulk
Content-Type: multipart/mixed; boundary=abcdefg
Accept: application/json

This is a message with multiple parts in MIME format.  This section is ignored.
--abcdefg
Content-Type: application/xml

<coffee><roast>french</roast><coffee>
--abcdefg
Content-Type: application/json

{ "type" : "coffee", "roast" : "french" }
--abcdefg--
```

### [Provider (for external data providers)](hub_channels_insert.html#provider)

```
POST http://{hub}/provider
Content-type: text/plain
Content-Encoding: gzip
Accept: application/json
channelName: {channel-name}
___body_contains_arbitrary_content
```

## Read

### [Specific item from channel](hub_channels_reading.html#specific)

`GET http://{hub}/channel/{channel-name}/{YYYY}/{MM}/{DD}/{hh}/{mm}/{ss}/{SSS}/{hash}`

### [Latest](hub_channels_reading.html#latest)

`GET http://{hub}/channel/{channel-name}/latest`

### [Earliest](hub_channels_reading.html#earliest)

`GET http://{hub}/channel/{channel-name}/earliest`

### [Next/Previoius Item](hub_channels_reading.html#next-and-previous)

Defaults to 1, but can suffix with /{n} to retrieve arbitrary number if items

`GET http://{hub}/channel/{channel-name}/{YYYY}/{MM}/{DD}/{hh}/{mm}/{ss}/{SSS}/{hash}/next[/{n}]`

`GET http://{hub}/channel/{channel-name}/{YYYY}/{MM}/{DD}/{hh}/{mm}/{ss}/{SSS}/{hash}/previous[/{n}]`

### [Bulk fetch](hub_channels_reading.html#bulk)

`GET http://{hub}/channel/{channel-name}/{YYYY}/{MM}/{DD}/{hh}/{mm}/{ss}/{SSS}/{hash}/{previous | next}[/{n}]?bulk=true`

### [Time interface](hub_channels_time.html)

`GET http://hub/channel/stumptown/time`

```json
{
    "_links": {
        "self": {
            "href": "http://hub/channel/stumptown/time"
        },
        "second": {
            "href": "http://hub/channel/stumptown/2014/12/23/05/58/55",
            "template": "http://hub/channel/stumptown/time/{year}/{month}/{day}/{hour}/{minute}/{second}{?stable}",
            "redirect": "http://hub/channel/stumptown/time/second"
        },
        "minute": {
            "href": "http://hub/channel/stumptown/2014/12/23/05/58",
            "template": "http://hub/channel/stumptown/time/{year}/{month}/{day}/{hour}/{minute}{?stable}",
            "redirect": "http://hub/channel/stumptown/time/minute"
        },
        "hour": {
            "href": "http://hub/channel/stumptown/2014/12/23/05",
            "template": "http://hub/channel/stumptown/time/{year}/{month}/{day}/{hour}{?stable}",
            "redirect": "http://hub/channel/stumptown/time/hour"
        },
        "day": {
            "href": "http://hub/channel/stumptown/2014/12/23",
            "template": "http://hub/channel/stumptown/time/{year}/{month}/{day}{?stable}",
            "redirect": "http://hub/channel/stumptown/time/day"
        }
    },
    "now": {
        "iso8601": "2014-12-23T05:59:00.162Z",
        "millis": 1419314340162
    },
    "stable": {
        "iso8601": "2014-12-23T05:58:55.000Z",
        "millis": 1419314335000
    }
}
```

### [List Tags](hub_channels_tag.html#list)

`GET http://hub/tag`

### [Tag Union (read from union of channels)](hub_channels_tag.html#union)

Examples:

```
GET http://hub/tag/coffee/latest
GET http://hub/tag/coffee/earliest
GET http://hub/tag/coffee/2015/06/25/16
GET http://hub/tag/coffee/2015/06/24/19/48/17/000/abc/previous
GET http://hub/tag/coffee/2015/06/24/19/48/17/000/abc/next/10
GET http://hub/channel/spella/2015/06/24/19/48/17/000/abc/next/10?tag=coffee
```

### Other Channel APIs 

* [Replication](hub_channels_replication.html)
* [Global](hub_channels_global.html)
* [Historical](hub_channels_historical)

## Notifications

### [Web Hooks]

#### [List existing](hub_notifications_webhooks.html#list) 

`GET http://hub/webhook`

#### [Create New](hub_notifications_webhooks.html#new) 

`GET http://hub/webhook`

``` json
{
  "callbackUrl" : "http://client/path/callback",
  "channelUrl" : "http://hub/channel/stumptown",
  "parallelCalls" : 2,
  "startItem" : "http://hub/channel/stumptown/2015/02/06/22/28/43/239/s03ub2",
  "paused" : false,
  "batch" : "SINGLE",
  "heartbeat" : false,
  "maxWaitMinutes" : 1,
  "ttlMinutes" : 0
}
```
