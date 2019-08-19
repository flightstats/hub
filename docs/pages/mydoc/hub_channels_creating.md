---
title: Creating and modifying channels
keywords: channel, create, update
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_creating.html
folder: hub
---

# Create a channel

* `name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9`, hyphen `-` and underscore `_`.
 Surrounding white space is trimmed (e.g. "  foo  " -> "foo" ).

* `owner` is optional and is limited to 48 characters.

* `ttlDays` and `maxItems` are optional, and only one can have a value greater than zero at a time. If neither is specified, a default value of 120 ttlDays is used.
Please see [channel limits](hub_channels_overview.html#channel-limits) for more details.  To change a channel to ttlDays or maxItems, set the other value explicitly to zero.

* `keepForever` overrides any `ttlDays` and `maxItems` settings.  Data inserted into a channel with keepForever=true will not be deleted.

* `description` is optional and defaults to an empty string.  This text field can be up to 1024 bytes long.

* `tags` is an optional array of string values.  Tag values are limited to 48 characters, and may only contain `a-z`, `A-Z` and `0-9`.
A channel may have at most 20 tags.

* `replicationSource` is the optional fully qualified path to channel in a another hub.  The data from the other channel
will be duplicated into this channel.  Please see [replication](hub_channels_replication.html) for more details.

* `storage` is the optional specification of how to store long term data.  The default is `BATCH`.  
High volume channels can see significant reductions in S3 costs by using `BATCH`.
`SINGLE` is primarily for mutableTime/historical channels and will be deprecated in the near future.
`BOTH` is a way to transition between the two states, and perform comparisons, 
it should not be used except for this and adds significant cost to your storage strategy, 
it will also be deprecated in the near future.  More information in [storage](hub_other_channel_storage.html)

* `protect` is the optional setting to prevent changes which might cause some data loss.
Please see [protected channels](hub_channels_protect.html) for more details.

`PUT http://hub/channel/stumptown`

* Content-type: application/json

```json
{
   "ttlDays": "14",
   "description": "a sequence of all the coffee orders from stumptown",
   "tags": ["coffee"]
}
```

On success:  `HTTP/1.1 201 OK`

```json
{
    "_links": {
        "self": {
            "href": "http://hub/channel/stumptown"
        },
        "latest": {
            "href": "http://hub/channel/stumptown/latest"
        },
        "earliest": {
            "href": "http://hub/channel/stumptown/earliest"
        },
        "bulk": {
             "href": "http://hub/channel/stumptown/bulk"
        },
        "ws": {
            "href": "ws://hub/channel/stumptown/ws"
        },
        "time": {
            "href": "http://hub/channel/stumptown/time"
        },
        "status" : {
              "href" : "http://hub/channel/load_test_1/status"
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlDays": 14,
    "description": "a sequence of all the coffee orders from stumptown",
    "tags": ["coffee"],
    "replicationSource": ""
}
```

Here's how you can do this with curl:
```bash
curl -i -X PUT http://hub/channel/stumptown

or

curl -i -X PUT --header "Content-type: application/json"  --data '{ "description" : "stumpy", "ttlDays" : 1 }' http://localhost:9080/channel/stumptown
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format
(currently, only `ttlDays`, `description`, `tags`, `owner` and `replicationSource` can be updated).
Each of these fields is optional.

`PUT http://hub/channel/channelname`

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://hub/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://hub/channel/stumptown`


{% include links.html %}
