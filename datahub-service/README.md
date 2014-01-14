datahub
=======

The FlightStats Data Hub

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

For the purposes of this document, the datahub is at http://deihub.

* On your local machine at: http://localhost:8080
* In development at: http://deihub.svc.dev.
* In staging at: http://deihub.svc.staging.
* In production at: http://deihub.svc.prod.

## overview

The DataHub is designed to be a fault tolerant, highly available service for data distribution.

It currently supports two types of Channels.

Sequence channels represent a linked list of data.  Each item added gets a sequential id.  They are traversable, and can support items up to 10 MB.
Sequence channels are intended for insertation rates up to one item per second.

TimeSeries channels are designed for small, high frequency items with low latency.  Each item added gets a unique id.  They are not traversable, and have a max item size of 60KB.
TimeSeries can support insertation rates up to 1000 items per second.  TimeSeries is higher throughput than Sequence, as well as slightly more expensive.

## list channels

To obtain the list of channels:

`GET http://deihub/channel`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://deihub/channel"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://deihub/channel/stumptown"
    }, {
      "name" : "ptown",
      "href" : "http://deihub/channel/ptown"
    } ]
  }
}
```
    
## create a channel

`name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.
Hyphens and underscores are not allowed in channel names. Surrounding white space is trimmed (e.g. "  foo  " -> "foo" ).

`type` is optional, and defaults to Sequence.  Valid values are Sequence and TimeSeries.

`ttlMillis` is optional and should be a positive number. If not specified, a default value (120 days) is used. If specified as null,
then the channel has no TTL.

`peakRequestRateSeconds` and `contentSizeKB` are optional, and are only used by TimeSeries to provision the throughput of the channel per second.
If the throughput is exceeded, the service will return an error code of ???.
todo - gfm - 1/13/14 - error code?

`POST http://deihub/channel`

* Content-type: application/json

```json
{
   "name": "stumptown",
   "type": "Sequence",
   "ttlMillis": "3600000"
   "peakRequestRateSeconds":1
   "contentSizeKB":10
}
//one hour in millis == 3600000
```

On success:  `HTTP/1.1 201 OK`

```json
{
    "_links": {
        "self": {
            "href": "http://deihub/channel/stumptown"
        },
        "latest": {
            "href": "http://deihub/channel/stumptown/latest"
        },
        "ws": {
            "href": "ws://deihub/channel/stumptown/ws"
        },
        "time": {
            "href": "http://deihub/channel/stumptown/time"
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlMillis": 3600000
    "type": "Sequence",
    "contentSizeKB" : 10,
    "peakRequestRateSeconds" : 10
}
```

Here's how you can do this with curl:
```bash
curl -i -X POST --header "Content-type: application/json" \
    --data '{"name": "stumptown"}'  \
    http://deihub/channel
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format (currently, only `ttlMillis`, `contentSizeKB` and `peakRequestRateSeconds` can be updated).
Each of these fields is optional.
Attempting to change other fields will result in a 400 error.

`PATCH http://deihub/channel/channelname`

* Content-type: application/json

```json
{
   "ttlMillis": "30000",
   "contentSizeKB" : 20,
   "peakRequestRateSeconds" : 5
}
```

On success:  `HTTP/1.1 200 OK`, and the new channel metadata is returned (see example return data from create channel).

Here's how you can do this with curl:
```bash
curl -i -X PATCH --header "Content-type: application/json" \
    --data '{"ttlMillis": "30000", "contentSizeKB" : 20, "peakRequestRateSeconds" : 5}'  \
    http://deihub/channel/stumptown
```

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://deihub/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://deihub/channel/stumptown`

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` and
`Content-Language` headers are optional:

```
POST http://deihub/channel/stumptown
Content-type: text/plain
Content-Language: en
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://deihub/channel/stumptown/1000`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://deihub/channel/stumptown"
    },
    "self" : {
      "href" : "http://deihub/channel/stumptown/1000"
    }
  },
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:

```bash
curl -i -X POST --header "Content-type: text/plain" \
    --data "your content here" \
    http://deihub/channel/stumptown
```

## fetch content from channel

To fetch content that was stored into a datahub channel, do a `GET` on the `self` link in the above response:

`GET http://deihub/channel/stumptown/1001`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://deihub/channel/stumptown/1000>;rel="previous"
Link: <http://deihub/channel/stumptown/1002>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
For Sequence channels, there are two `Link` headers that provide links to the previous and next items in the channel.
If you are fetching the latest item, there will be no next link.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://deihub/channel/stumptown/1000`

## fetch latest channel item

To retrieve the latest item inserted into a Sequence channel, issue a HEAD or GET request on the `latest` link returned from the channel
metadata.  The datahub will issue a 303 redirect.
This feature is not supported by TimeSeries channels.

`HEAD http://deihub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://deihub/channel/stumptown/00002FHSQESAS000`

Here is how you can do this with curl:

`curl -I http://deihub/channel/stumptown/latest`

## time interface

The time interface returns all of the URIs of items inserted within the specified minute.

To see the time format, issue a HEAD or GET request on the `time` link returned from the channel metadata.
The datahub will issue a 303 redirect for the current time.

`HEAD http://deihub/channel/stumptown/time`

On success:  `HTTP/1.1 303 See Other`
`Location: http://deihub/channel/stumptown/time/2014-01-13T18:42-0800`

A GET on the returned URL will return all of the content URIs within that period.
The time format is the ISO 8601 extended format with minute resolution.  In Java it is "yyyy-MM-dd'T'HH:mmZ" and in Javascript using Moment.js it is "YYYY-MM-DDTHH:mmZ"

`GET http://deihub/channel/stumptown/time/2014-01-13T19:13-0800`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://deihub/channel/stumptown/time/2014-01-13T19:13-0800"
    },
    "uris" : [ "http://deihub/channel/stumptown/1002", "http://deihub/channel/stumptown/1003",
    "http://deihub/channel/stumptown/1004", "http://deihub/channel/stumptown/1005",
    "http://deihub/channel/stumptown/1006", "http://deihub/channel/stumptown/1007" ]
  }
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items submitted within the minute so far, and may
change if other items are submitted.

## subscribe to events

While a common approach to consuming data from the datahub involves traversing next/previous links, clients may
"subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Once connected, the line-oriented protocol is simple:

For Sequence channels, each time data is inserted into the channel, the datahub will send a line to the client with the
URL for that content.

```
http://deihub/channel/stumptown/1000
http://deihub/channel/stumptown/1001
http://deihub/channel/stumptown/1002
...etc...
```

For TimeSeries channels, the websocket will return the time URL that is available
```
http://deihub/channel/stumptownTime/2014-01-13T18:40-0800
http://deihub/channel/stumptownTime/2014-01-13T18:41-0800
http://deihub/channel/stumptownTime/2014-01-13T18:42-0800
...etc...
```


