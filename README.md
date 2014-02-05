The Hub
=======

* [overview](#overview)
* [list channels](#list-channels)
* [create a channel](#create-a-channel)
* [update a channel](#update-a-channel)
* [fetch channel metadata](#fetch-channel-metadata)
* [insert content into channel](#insert-content-into-channel)
* [fetch content from channel](#fetch-content-from-channel)
* [fetch latest channel item](#fetch-latest-channel-item)
* [time interface](#time-interface)
* [subscribe to events](#subscribe-to-events)
* [provider interface](#provider-interface)
* [delete a channel](#delete-a-channel)
* [configure replication](#configure-replication)
* [replication status](#replication-status)
* [stop replication](#stop-replication)
* [api updates](#api-updates)
* [development](#development)

For the purposes of this document, the Hub is at http://hub/.

* On your local machine it is at: http://localhost:8080/
* In development: http://hub.svc.dev/
* In staging: http://hub.svc.staging/
* In production: http://hub.svc.prod/

## overview

The Hub is designed to be a fault tolerant, highly available service for data distribution.

It currently supports two types of Channels, Sequence and TimeSeries.

Sequence channels represent a linked list of data.  Each item added gets a sequential id.  They are traversable, and can support items up to 10 MB.
Sequence channels are intended for insertation rates less than five items per second.
Users should note that with high frequency inserts, the clients view of insertion order may not be maintained.

TimeSeries channels are designed for small, high frequency inserts with low latency.  Each item added gets a unique id.  They are not traversable, and have a max content size of 60KB.
TimeSeries can support insertation rates up to 1000 items per second.
TimeSeries is higher throughput and lower latency than Sequence, as well as slightly more expensive.
TimeSeries also requires the users to know the frequency and size of inserts.

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

`name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.
Hyphens are not allowed in channel names. Surrounding white space is trimmed (e.g. "  foo  " -> "foo" ).

`type` is optional, and defaults to Sequence.  Valid values are Sequence and TimeSeries.

`ttlDays` is optional and should be a positive number. If not specified, a default value of 120 days is used.
`ttlMillis` is still accepted as an input parameter, and is converted to ttlDays.  A `null` ttlMillis is converted to 1000 years.

`peakRequestRateSeconds` and `contentSizeKB` are optional, and are only used by TimeSeries to provision the throughput of the channel per second.
If the throughput is exceeded, the service will return an error code of 503 with a `Retry-After` header providing a value in seconds.

`POST http://hub/channel`

* Content-type: application/json

```json
{
   "name": "stumptown",
   "type": "Sequence",
   "ttlDays": "14",
   "peakRequestRateSeconds":1,
   "contentSizeKB":10
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
        "ws": {
            "href": "ws://hub/channel/stumptown/ws"
        },
        "time": {
            "href": "http://hub/channel/stumptown/time"
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlDays": 14,
    "type": "Sequence",
    "contentSizeKB" : 10,
    "peakRequestRateSeconds" : 10
}
```

Here's how you can do this with curl:
```bash
curl -i -X POST --header "Content-type: application/json" \
    --data '{"name": "stumptown"}'  \
    http://hub/channel
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format (currently, only `ttlDays`, `contentSizeKB` and `peakRequestRateSeconds` can be updated).
Each of these fields is optional.
Attempting to change other fields will result in a 400 error.

`PATCH http://hub/channel/channelname`

* Content-type: application/json

```json
{
   "ttlDays": 21,
   "contentSizeKB" : 20,
   "peakRequestRateSeconds" : 5
}
```

On success:  `HTTP/1.1 200 OK`, and the new channel metadata is returned (see example return data from create channel).

Here's how you can do this with curl:
```bash
curl -i -X PATCH --header "Content-type: application/json" \
    --data '{"ttlDays": 21, "contentSizeKB" : 20, "peakRequestRateSeconds" : 5}'  \
    http://hub/channel/stumptown
```

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://hub/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://hub/channel/stumptown`

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` and
`Content-Language` headers are optional:

```
POST http://hub/channel/stumptown
Content-type: text/plain
Content-Language: en
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://hub/channel/stumptown/1000`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://hub/channel/stumptown"
    },
    "self" : {
      "href" : "http://hub/channel/stumptown/1000"
    }
  },
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:

```bash
curl -i -X POST --header "Content-type: text/plain" \
    --data "your content here" \
    http://hub/channel/stumptown
```

## fetch content from channel

To fetch content that was stored into a hub channel, do a `GET` on the `self` link in the above response:

`GET http://hub/channel/stumptown/1001`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://hub/channel/stumptown/1000>;rel="previous"
Link: <http://hub/channel/stumptown/1002>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
For Sequence channels, there are two `Link` headers that provide links to the previous and next items in the channel.
If you are fetching the latest item, there will be no next link.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://hub/channel/stumptown/1000`

## fetch latest channel item

To retrieve the latest item inserted into a Sequence channel, issue a HEAD or GET request on the `latest` link returned from the channel
metadata.  The Hub will issue a 303 redirect.
This feature is not supported by TimeSeries channels.

`HEAD http://hub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/00002FHSQESAS000`

Here is how you can do this with curl:

`curl -I http://hub/channel/stumptown/latest`

## time interface

The time interface returns all of the URIs of items inserted within the specified minute.

To see the time format, issue a GET request on the `time` link returned from the channel metadata.
The Hub will issue a 303 redirect for the current time.

`HEAD http://hub/channel/stumptown/time`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/time/2014-01-13T10:42+0000`

A GET on the returned URI will return all of the content URIs within that period.
The time format is the ISO 8601 extended format with minute resolution.  In Java it is "yyyy-MM-dd'T'HH:mmZ" and in Javascript using Moment.js it is "YYYY-MM-DDTHH:mmZ"

`GET http://hub/channel/stumptown/time/2014-01-13T12:42-0800`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel/stumptown/time/2014-01-13T12:42-0800"
    },
    "uris" : [ "http://hub/channel/stumptown/1002", "http://hub/channel/stumptown/1003",
    "http://hub/channel/stumptown/1004", "http://hub/channel/stumptown/1005",
    "http://hub/channel/stumptown/1006", "http://hub/channel/stumptown/1007" ]
  }
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items inserted within the minute so far, and will
change as other items are inserted.

## subscribe to events

While a common approach to consuming data from the hub involves traversing next/previous links, clients may
"subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Clients should be aware that websockets are a "best effort" service, and are not stateful.  If the ws connection is lost,
which will happen on hub server restarts, the client will need to reconnect, and may have missed items.

Once connected, the line-oriented protocol is simple:

For Sequence channels, each time data is inserted into the channel, the hub will send a line to the client with the
URL for that content.

```
http://hub/channel/stumptown/1000
http://hub/channel/stumptown/1001
http://hub/channel/stumptown/1002
...etc...
```

Currently TimeSeries channels do not support websockets.  If this is a desired feature, please discuss it with us.

## provider interface

For external data providers, there is a simplified interface suitable for exposing to the authenticated outside world.

`POST http://hub/provider/`

* it creates a Sequence channel if it doesn't exist
* it expects a `channelName` header
* does not support any other HTTP methods
* does not return any links
* access by external data providers is controlled through a proxy maintained by Operations

## delete a channel

To delete a channel when after you no longer need it, simply issue a `DELETE` command to that channel.

 `DELETE http://hub/channel/stumptown`

Here's how you can do this with curl:
```bash
curl -i -X DELETE http://hub/channel/stumptown
```

## configure replication

The Hub can replicate Sequence channels from another Hub instance.  TimeSeries replication is not yet supported.

To configure new or modify existing replication of channels in the domain 'hub.other':

`PUT http://hub/replication/hub.other`

* Content-type: application/json

```json
{
   "historicalDays" : 10,
   "excludeExcept" : [ "stumptown", "pdx" ]
}
```
* Modifications to existing replication configuration take effect immediately.
* `excludeExcept` means "Exclude all of the channels, Except the ones specified".
* `includeExcept` means "Include all of the channels, Except the ones specified".  This will pick up new channels which aren't in the except list.
* `includeExcept` and `excludeExcept` are mutually exclusive.  Attempts to set both will result in a 400 response code.
* `historicalDays` tells the replicator how far back in time to start. Zero means "only get new values".
* If http://hub.other/channel/stumptown `ttlDays` is 10, and `historicalDays` is 5, only items from the last 5 days will be replicated.
* If a channel's `historicalDays` is 0 and the ongoing replication is restarted, replication will continue with the existing sequence if it is up to `historicalDays` + 1 old.

As an example, the replicating cluster is going into a maintenance window, and a domain has `historicalDays` of zero.  The entire cluster is stopped at noon.
Maintenance takes longer than expected and the cluster resumes at 11 AM the next day.  Replication will pick up where it left off, and will eventually catch up to the current position.
If, instead, the cluster didn't start until 1 PM the next day, one hour's worth will not be replicated.

## replication status

You can get the status of the current replication processes at:

 `GET http://hub/replication`

 ```json
 {
   "domains" : [ {
     "domain" : "hub.other",
     "historicalDays" : 10,
     "includeExcept" : [ ],
     "excludeExcept" : [ "stumptown", "pdx" ]
   } ],
  "status" : [ {
       "url" : "http://hub.other/channel/stumptown",
       "replicationLatest" : 310491,
       "sourceLatest" : 310491,
       "delta" : 0
     }, {
       "url" : "http://hub.other/channel/pdx",
       "replicationLatest" : 170203,
       "sourceLatest" : 184203,
       "delta" : 14000
     } ]
 }
 ```

## stop replication

Stop replication of the entire domain `hub.other`, issue a `DELETE` command.

`DELETE http://hub/replication/hub.other`


## api updates

Also, there are a couple of small API changes that clients should be aware of.

`ttlMillis` is deprecated, users should use `ttlDays` instead.  Existing `ttlMillis` values are rounded up to the nearest day.

`lastUpdated` is no longer provided in the channel metadata.  If you need to know the latest value, you can use the `/latest` interface

## development

The Hub is a work in progress.  If you'd like to contribute, let us know.

The latest builds are in Jenkins - http://ops-jenkins01.util.pdx.office/job/hub/

To run Java based tests and jasmine-node tests locally, you will most likely want to use DynamoDB Local.

Install from http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html
and then start it running.

Once it's running, hijack src/main/resources/default.properties and make the following changes

#dynamo.endpoint=dynamodb.us-east-1.amazonaws.com
dynamo.endpoint=localhost:8000

To run the jasmine-node based integration tests:
jasmine-node .
