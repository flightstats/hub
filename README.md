The Hub
=======

* [overview](#overview)
* [consistency](#consistency)
* [clients](#clients)
* [error handling](#error-handling)
* [FAQ](#faq)
* [hub resources](#hub-resources)
* [list channels](#list-channels)
* [create a channel](#create-a-channel)
* [update a channel](#update-a-channel)
* [fetch channel metadata](#fetch-channel-metadata)
* [insert content into channel](#insert-content-into-channel)
* [bulk insert content into channel](#bulk-insert-content-into-channel)
* [fetch content from channel](#fetch-content-from-channel)
* [latest channel item](#latest-channel-item)
* [earliest channel item](#earliest-channel-item)
* [next and previous links](#next-and-previous-links)
* [fetch bulk content from channel](#fetch-bulk-content-from-channel)
* [channel status](#channel-status)
* [channel limits](#channel-limits)
* [tag interface](#tag-interface)
* [tag unions](#tag-unions)
* [time interface](#time-interface)
* [subscribe to events](#subscribe-to-events)
* [group callback](#group-callback)
* [provider interface](#provider-interface)
* [delete a channel](#delete-a-channel)
* [replication](#replication)
* [alerts](#alerts)
* [health check](#health-check)
* [storage](#storage)
* [access control](#access-control)
* [encrypted-hub](#encrypted-hub)
* [monitoring](#monitoring)
* [development](#development)
* [deployments](#deployments)
* [Requirements Notes](#Requirements-Notes)



## overview

The Hub is designed to be a fault tolerant, highly available service for data storage and distribution.  All features are available via a REST API.

It supports channels of data ordered by time.
Channels represent uniquely addressable items that are iterable and query-able by time.  Each item may be up to to 20 MB.

The [encrypted-hub](#encrypted-hub) (EH) is a separate installation of the Hub which ensures that all communication is encrypted.

## consistency

* All times from the Hub are in UTC.
* By default all iteration, queries, group callbacks and websockets return items with stable ordering.  Data is considered stable when iteration will provide consistent results.
* All requests for a specific item by id will return that item if it exists.

## error handling

Clients should consider handling transient server errors (500 level return codes) with retry logic.  This helps to ensure that transient issues (networking, etc)
  do not prevent the client from entering data. For Java clients, this framework provides many options - https://github.com/rholder/guava-retrying
We also recommend clients use exponential backoff for retries.

## FAQ

* Why does /latest (or /earliest) return 404?

  Either data has never been added to that channel, or the last data added to that channel is older than the time to live (ttlDays).

* How can I guarantee ordering for items within a channel?

  You can wait for the response for an item before writing the next item.  

## hub resources

To explore the Resources available in the Hub, go to http://hub/

**Note**
For the purposes of this document, the Hub is at http://hub/.
On your local machine it is at: http://localhost:9080/

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
    
## create a channel

* `name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9`, hyphen `-` and underscore `_`.
 Surrounding white space is trimmed (e.g. "  foo  " -> "foo" ).

* `owner` is optional and is limited to 48 characters.

* `ttlDays` and `maxItems` are optional, and only one can be specified at a time. If neither is specified, a default value of 120 ttlDays is used.
Please see [channel limits](#channel-limits) for more details.

* `description` is optional and defaults to an empty string.  This text field can be up to 1024 bytes long.

* `tags` is an optional array of string values.  Tag values are limited to 48 characters, and may only contain `a-z`, `A-Z` and `0-9`.
A channel may have at most 20 tags.

* `replicationSource` is the optional fully qualified path to channel in a another hub.  The data from the other channel
will be duplicated into this channel.  Please see [replication](#replication) for more details.

* `storage` is the optional specification of how to store long term data.  The default is `SINGLE`.  
High volume channels can see significant reductions in S3 costs by using `BATCH`.  
`BOTH` is a way to transition between the two states, and perform comparisons.  More information in [storage](#storage)

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

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` header is optional:

```
POST http://hub/channel/stumptown
Content-type: text/plain
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://hub/channel/stumptown"
    },
    "self" : {
      "href" : "http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}"
    }
  },
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:

```bash
curl -i -X POST --header "Content-type: text/plain" --data 'your content here' http://hub/channel/stumptown
```

## bulk insert content into channel

You can also insert items in bulk into the hub, and each item will receive a unique ordered uri.

Currently, [MIME](https://tools.ietf.org/html/rfc2045) is the only way to insert bulk items.
Issue a POST on the channel's `bulk` URI and specify the appropriate "multipart" Content-Type.

Notes on MIME:
* All lines must be terminated by [CRLF](https://tools.ietf.org/html/rfc2045#section-2.1)
* Anything before the starting boundary is ignored 
* An empty line after the optional Content headers starts the message body
* binary payloads are not currently supported

```
POST http://hub/channel/stumptown/bulk
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

On success: `HTTP/1.1 201 Created`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://hub/channel/stumptown"
    },
    "uris" : [
      "http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}000",
      "http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}001"
    ]
  }
}
```

## fetch content from channel

To fetch content that was stored into a hub channel, do a `GET` on the `self` link in the above response:

`GET http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}/previous>;rel="previous"
Link: <http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}/next>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
There are two `Link` headers that provide links to the previous and next items in the channel.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

## fetch latest channel item

To retrieve the latest item inserted into a channel, issue a HEAD or GET request on the `latest` link 
returned from the channel metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

Here is how you can do this with curl:

`curl -I http://hub/channel/stumptown/latest`

You can also retrieve the latest N items by using /latest/{n}

## fetch earliest channel item

To retrieve the earliest item inserted into a channel, issue a HEAD or GET request on the `earliest` link 
returned from the channel metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub/channel/stumptown/earliest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

Here is how you can do this with curl:

`curl -I http://hub/channel/stumptown/earliest`

You can also retrieve the earliest N items by using /earliest/{n}

## next and previous links

A GET on the next and previous links returned as headers with content will redirect to those respective items.  A 404 will be returned if they don't exist.

Any item's uri can be appended with /next or /previous to navigate forward or backward.
If you append a number /next/20 or /previous/15, and you'll receive a list of that many items.

For example:

`GET http://hub/channel/stumptown/2014/12/23/23/14/50/514/xIXX5L/previous/3`

On success: `HTTP/1.1 200 OK`
```
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel/stumptown/2014/12/23/23/14/50/514/xIXX5L/previous/3"
    },
    "next" : {
      "href" : "http://hub/channel/stumptown/2014/12/23/23/14/49/887/x46z8p/next/3"
    },
    "previous" : {
      "href" : "http://hub/channel/stumptown/2014/12/23/23/14/42/751/mRklXw/previous/3"
    },
    "uris" : [
        "http://hub/channel/stumptown/2014/12/23/23/14/47/376/swdWJD", 
        "http://hub/channel/stumptown/2014/12/23/23/14/48/115/lDCHYY", 
        "http://hub/channel/stumptown/2014/12/23/23/14/49/887/x46z8p" 
        ]
  }
}
```

## fetch bulk content from channel

Any query operation (including next, previous, earliest, latest, and times) supports the addition of the
query parameter `?bulk=true`.  Using the bulk parameter will result in the content of the query items being streamed
as [MIME](https://tools.ietf.org/html/rfc2045) or as a zip file to the client.
To get a zip file, specify the content-type as `application/zip`.

*NOTE* `bulk` was previously named `batch`.  `batch` is deprecated. 

The hub will generate a random 70 character boundary, and follows the same MIME rules as [bulk insert content into channel](#bulk-insert-content-into-channel)

Using the previous example:
          
`GET http://hub/channel/stumptown/2014/12/23/23/14/50/514/xIXX5L/previous/3?bulk=true`

On success: `HTTP/1.1 200 OK`

header:
```
content-type: multipart/mixed; boundary=||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||
```

body:
```
--||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||
Content-Type: application/json
Content-Key: http://hub/channel/stumptown/2014/12/23/23/14/47/376/swdWJD

{ "type" : "coffee", "roast" : "french" } 
--||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||
Content-Type: application/json
Content-Key: http://hub/channel/stumptown/2014/12/23/23/14/48/115/lDCHYY

{ "type" : "coffee", "roast" : "italian" }
--||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||
Content-Type: application/json
Content-Key: http://hub/channel/stumptown/2014/12/23/23/14/49/887/x46z8p
 
{ "type" : "coffee", "roast" : "hair bender" } 
--||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||--
```

## channel status

A GET on the status link for a channel will return the link to the latest item in the channel.
If a replicationSource is defined, it will also return the link the the latest in the replication hub.

`GET http://hub/channel/stumptown/status`

On success: `HTTP/1.1 200 OK`
```
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

## channel limits

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

## tag interface

Tags are used to logically group channels.  To retrieve all of the tags in the Hub:

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

## tag unions

Tags can also be used for a read-only union set of all it's channels.
[next and previous links](#next-and-previous-links), [latest](#latest-channel-item), 
[earliest](#earliest-channel-item) and [time](#time-interface) work the same as their channel analogs.
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

## time interface

The time interface provides a variety of options to query data in a hub channel.  All queries will only show items
 with stable ordering by default.  If you want to see items which might be unstable, add the parameter ```?stable=false```

To see time format options, issue a GET request on the `time` link returned from the channel metadata.

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

Call a named uri, and the Hub will issue a 303 redirect for the current time with the specified resolution.

`HEAD http://localhost:9080/channel/stumptown/time/second`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/2014/01/13/10/42/31

A GET on the returned Location will return all of the content URIs within that period.

`GET http://hub/channel/stumptown/2014/01/13/10/42/31`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel/stumptown/2014/01/13/10/42/31"
    },
    "uris" : [ 
        "http://hub/channel/stumptown/2014/01/13/10/42/31/149/{hash1}", 
        "http://hub/channel/stumptown/2014/01/13/10/42/31/359/{hash2}",
        "http://hub/channel/stumptown/2014/01/13/10/42/31/642/{hash3}" 
    ]
  }
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items inserted within the minute so far, and will
increase as other items are inserted.

### time resolution

You can request all of the items by the time resolution you specify in the URL.  
For all the items in a minute: `GET http://hub/channel/stumptown/2014/01/13/10/42`
For all the items in an hour: `GET http://hub/channel/stumptown/2014/01/13/10`

You can also access the urls via convenience methods:

`HEAD http://hub/channel/stumptown/time/minute`
`HEAD http://hub/channel/stumptown/time/hour`

The output format is the same regardless of time resolution

## subscribe to events

Clients may "subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Clients should be aware that websockets are a "best effort" service, and are not stateful.  If the ws connection is lost,
which will happen on hub server restarts, the client will need to reconnect, and may have missed items.

Once connected, the line-oriented protocol is simple:

Each time data is inserted into the channel, the hub will send a line to the client with the
URL for that content.

```
http://hub/channel/stumptown/2014/01/13/10/42/31/149/{hash1}
http://hub/channel/stumptown/2014/01/13/10/42/31/359/{hash2}
http://hub/channel/stumptown/2014/01/13/10/42/31/642/{hash3}
...etc...
```

The returned items are stable only.

## group callback

The Group Callback mechanism is an alternative to WebSockets for consuming events.  This POSTs json uris via HTTP, and
the Hub server keeps track of the Group's state.

* `name` is used in the url for the callback.  Names are limited to 48 characters and may only contain `a-z`, `A-Z`, `0-9`, hyphen `-` and underscore `_`.

* `callbackUrl` is the fully qualified location to receive callbacks from the server.

* `channelUrl` is the fully qualified channel location to monitor for new items.

* `parallelCalls` is the optional number of callbacks to make in parallel.  The default value is `1`.
If parallelCalls is higher than one, callback ordering is not guaranteed.
parallelCalls can be modified with a call to PUT 

* `startItem` is the optional fully qualified item location where the callback should start from.  The startItem will not be sent.
startItem is *only* used when creating a group callback.  If you want to change the pointer of a callback, you will need to
delete the callback first.

* `paused` is optional and defaults to false.   When true, this will pause a group callback.

* `batch` is optional and defaults to `SINGLE`, which will return each item by itself.
  Setting the value to `MINUTE` will return each minute's worth of data in the channel.  MINUTE callbacks will return an empty 
  array of uris if there are no items.  

* `heartbeat` is optional and defaults to false for `SINGLE`. `MINUTE` batches always have a heartbeat.
   A heartbeat is a callback which identifies the end of a minute period.  It may have an empty `uris` array.
   It will include an `id` field which identifies the ending minute.
   
* `maxWaitMinutes` is optional and defaults to 1.  maxWaitMinutes is the maximum amount of time between retry attempts to the callbackUrl.

* `ttlMinutes` is optional and defaults to 0.  If ttlMinutes is greater than 0, the hub will not attempt to send an item which is older than the ttl.

To get a list of existing group callbacks:

`GET http://hub/group`
 
To create a new group callback:

`PUT http://hub/group/{name}`

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

Once a Group is created, the channelUrl can not change.  PUT may be safely called multiple times with the same
 configuration.  Changes to `startItem` and `batch` will be ignored.

To see the configuration and status of a group callback:

`GET http://hub/group/{name}`

To delete a group callback:

`DELETE http://hub/group/{name}`

DELETE will return a 202, and it may take up to a minute to properly stop a group from servicing the callback.

#### Behavior

The application listening at `callbackUrl` will get a payload POSTed to it for every new item in the channel, starting after `startItem` or at the time the group is created.
A 200 client response is considered successful.  Any other response is considered an error, and will cause the server to retry.   Redirects are allowed.                                        
Retries will use an exponential backoff up to one minute, and the server will continue to retry at one minute intervals indefinitely.

An example SINGLE payload:

``` json
{
  "name" : "stumptownCallback",
  "type" : "item",
  "uris" : [ "http://hub/channel/stumptown/2014/01/13/10/42/31/759/s03ub2" ]
}
```

An example SINGLE heartbeat:

``` json
{
  "name" : "stumptownCallback",
  "type" : "heartbeat",
  "id" : "2014/01/13/10/42",
  "uris" : []
}
```

An example MINUTE payload:

``` json
{
  "name" : "stumptownCallbackBatch",
  "type" : "items",
  "id" : "2014/01/13/10/42",
  "url" : "http://hub/channel/stumptown/2014/01/13/10/42",
  "bulkUrl" : "http://hub/channel/stumptown/2014/01/13/10/42?bulk=true",
  "uris" : [ 
    "http://hub/channel/stumptown/2014/01/13/10/42/05/436/abcdef",
    "http://hub/channel/stumptown/2014/01/13/10/42/31/759/s03ub2"
    "http://hub/channel/stumptown/2014/01/13/10/42/39/029/zxcvbn"
  ]
}
```

An example MINUTE heartbeat :

``` json
{
  "name" : "stumptownCallbackBatch",
  "type" : "heartbeat",
  "id" : "2014/01/13/10/42",
  "url" : "http://hub/channel/stumptown/2014/01/13/10/42",
  "bulkUrl" : "http://hub/channel/stumptown/2014/01/13/10/42?bulk=true",
  "uris" : [ ]
}
```

## provider interface

For external data providers, there is a simplified interface suitable for exposing to the authenticated outside world.

`POST http://hub/provider/`

* it creates a channel if it doesn't exist
* it expects a `channelName` header
* does not support any other HTTP methods
* does not return any links
* access by external data providers is controlled through a proxy maintained by Operations

## delete a channel


To delete a channel when after you no longer need it, simply issue a `DELETE` command to that channel.
Delete returns a 202, indicating that the request has been accepted, and will take an indeterminate time to process.
If you re-create a channel before all the data has been deleted, the behavior is undefined.

 `DELETE http://hub/channel/stumptown`

Here's how you can do this with curl:
```bash
curl -i -X DELETE http://hub/channel/stumptown
```

## replication

The hub can replicate a source channel from another hub instance into a destination channel.  The destination channel can have any name.

To configure replication, specify `replicationSource` when creating the new channel in the desired destination.

To stop replication, either delete the destination channel, or PUT the destination channel with a blank `replicationSource`.

Modifications to configuration takes effect immediately.

Replication destination channels do not allow inserts.

## alerts

The hub can send alerts based on the number of items in a channel, or how long a group callback is lagging a channel.

For channels, an alert is created if inserts in `source` `operator` `threshold` within `timeWindowMinutes`
eg: if inserts in stumptown <  100 within 20 minutes

For group callbacks, an alert is created if the callback `source` lags behind it's channel by `timeWindowMinutes`
eg: if the last completed callback to stumptownCallback is 10 minutes behind the last insert into it's channel

* `name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.

* `source` is the name of the channel or group to monitor

* `serviceName` is a user defined end point for the alert, which could be an email address, service name, etc

* `type` can be `channel` or `group`

* `timeWindowMinutes` the period of time to evaluate

* `operator` (channel only) can be `>=`, `>`, `==`, `<`, or `<=`

* `threshold` (channel only) is the value to compare

### create or change and alert

Alerts can be created and changed with PUT 

`PUT http://hub/alert/stumptownAlert`

* Content-type: application/json

```json
{
    "source": "stumptown",
    "serviceName": "stumptown@example.com",
    "timeWindowMinutes": 5,
    "type": "channel",
    "operator": "==",
    "threshold": 0
}
```

On success:  `HTTP/1.1 201 OK`

```json
{
    "name": "stumptownAlert",
    "source": "stumptown",
    "serviceName": "stumptown@example.com",
    "timeWindowMinutes": 5,
    "type": "channel",
    "operator": "==",
    "threshold": 0
    "_links": {
        "self": {
            "href": "http://hub-v2.svc.prod/alert/stumptownAlert"
        },
        "status": {
            "href": "http://hub-v2.svc.prod/alert/stumptownAlert/status"
        }
    }
}
```

### channel alert status

Following the status link from _links.status.href shows the channel history for the current state of the alert

`GET http://hub-v2.svc.prod/alert/stumptownAlert/status`

```json
{
    "name": "stumptownAlert",
    "period": "minute",
    "alert": true,
    "type": "channel",
    "history": [
    {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/19/21?stable=true",
        "items": 0
    },
    {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/19/22?stable=true",
        "items": 0
    },
    {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/19/23?stable=true",
        "items": 0
    },
    {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/19/24?stable=true",
        "items": 0
    },
    {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/19/25?stable=true",
        "items": 0
    }
    ],
    "_links": {
        "self": {
            "href": "http://hub-v2.svc.prod/alert/stumptownAlert/status"
        }
    }
}
```

### group alert status

Following the status link from _links.status.href shows the latest item in a channel, and the last completed callback for that group.

`GET http://hub-v2.svc.prod/alert/stumptownGroup/status`

```
{
    "name": "stumptownGroup",
    "alert": false,
    "type": "group",
    "history": [
        {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/18/34/38/306/UqCNR4",
        "name": "channelLatest"
        },
        {
        "href": "http://hub-v2.svc.prod/channel/stumptown/2015/06/17/18/34/38/306/UqCNR4",
        "name": "lastCompletedCallback"
        }
    ],
    "_links": {
        "self": {
            "href": "http://hub-v2.svc.prod/alert/stumptownGroup/status"
        }
    }
}
```
## health check

The Health Check returns a 200 status code when the server can connect to each data store.
If the server can not access a data store, it will return a 500 status code.

Responding to http connections is the last step on startup, so the health check will be unresponsive until startup is complete.
On shutdown, the server immediately stops responding to new http connections, so there are no separate codes for startup and shutdown.

`GET http://hub/health`

```json
{
  "healthy" : true,
  "description" : "OK",
  "version" : "2014-03-26.126"
}
```

## storage

The Hub has two options to store data:
* It can use a combination of a local cache and [S3](https://aws.amazon.com/s3/)
* It can use a single drive shared across the cluster
 
For Hubs which use S3, the channel option `storage` can make a significant difference in costs.
High volume channels should prefer `BATCH` to reduce costs.



## access control

Currently, all access to the Hub is uncontrolled, and does not require authentication.
Over time, access control will be added to some of the API, starting with Channel Deletion and Replication Management.
To request a change to a controlled API, or to request access, please use the [hub-discuss forum](https://groups.google.com/a/conducivetech.com/forum/#!forum/hub-discuss)

## encrypted-hub

The Encrypted Hub (EH) is a separate installation of The Hub.
EH also has some additional features to the normal Hub:

* All channel items are encrypted at rest (this relies on disk level encryption)
* All channel items are encrypted in flight
* All access to channel items (reads and writes) require authentication and are access controlled

Channel inserts can be audited by a GET or HEAD to each channel item.  The creator of the record is returned in a `User` header.

## development

The Hub is a work in progress.  If you'd like to contribute, let us know.

[Install locally](https://github.com/flightstats/hubv2/wiki/Install-hub-locally)

General Rules for Development:
* Only pull from master
* Create a new branch for features and bugs, avoiding '/' in the branch name
* after testing, create a pull request from the feature branch to master

