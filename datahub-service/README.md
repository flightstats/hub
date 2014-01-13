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
* [subscribe to events](#subscribe-to-events)
* [time index](#time-index)

For the purposes of this document, the datahub is at http://deihub.

On your local machine, , it is actually at: http://localhost:8080
In development, it is actually at: http://deihub.svc.dev.
In staging, it is actually at: http://deihub.svc.staging.
In production, it is actually at: http://deihub.svc.prod.

## overview

The DataHub is designed to be a fault tolerant, highly available service for data distribution.

It currently supports two types of Channels.
Sequence channels represent and linked list of data.  Each item added gets a sequential id.  They are traversable, and can support items up to 10 MB.
Sequence channels are intended for insertation rates up to one item per second.

TimeSeries channels are designed for small, high frequency items with low latency.  Each item added gets a unique id.  They are not traversable, and have a max item size of 60KB.
TimeSeries can support insertation rates up to 1000 items per second.  TimeSeries is more expensive than Sequence

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

todo - gfm - 1/13/14 - info on channel types
//todo - gfm - 1/13/14 - add other fields from ChannelConfiguration

Channel names _are case sensitive_, are limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.
Hyphens and underscores are not allowed in channel names. Surrounding white space is trimmed (e.g. "  foo  " -> "foo" ).

type is optional, and defaults to Sequence.  Valid values are Sequence and TimeSeries.

ttlMillis is optional and should be a positive number. If not specified, a default value (120 days) is used. If specified as null,
then the channel has no TTL.

peakRequestRateSeconds and contentSizeKB are optional, and are only used by TimeSeries.  They are used to provision the throughput of the channel per second.
If the throughput is exceeded, the service will return an error (return code?).


`POST http://deihub/channel`

* Content-type: application/json

```json
{
   "name": "stumptown",
   "type"" "Sequence",
   "ttlMillis": "3600000"
   "peakRequestRateSeconds":1
   "contentSizeKB":10
}
//one hour in millis == 3600000
```

On success:  `HTTP/1.1 201 OK`
//todo - gfm - 1/13/14 - add other fields from ChannelConfiguration

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
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlMillis": 3600000
}
```

Here's how you can do this with curl:
```bash
curl -i -X POST --header "Content-type: application/json" \
    --data '{"name": "stumptown"}'  \
    http://deihub/channel
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format (currently, only TTL can be updated).

//todo - gfm - 1/13/14 - add other fields from ChannelConfiguration

`PATCH http://deihub/channel/channelname`

* Content-type: application/json

```json
{
   "ttlMillis": "30000" //30 seconds
}
```

On success:  `HTTP/1.1 200 OK`, and the new channel metadata is returned (see example return data from create channel).

Here's how you can do this with curl:
```bash
curl -i -X PATCH --header "Content-type: application/json" \
    --data '{"ttlMillis": "30000"}'  \
    http://deihub/channel/stumptown
```

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://deihub/channel/stumptown`

On success: `HTTP/1.1 200 OK`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://deihub/channel/stumptown"
    },
    "latest" : {
      "href" : "http://deihub/channel/stumptown/latest"
    },
    "ws" : {
      "href" : "ws://deihub/channel/stumptown/ws"
    }
  },
  "ttlMillis" : "30000",
  "name" : "stumptown",
  "creationDate" : "2013-04-23T20:36:17.885Z"
}
```

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
Also note that there are two `Link` headers that provide links to the previous and next items in the channel.
If you are fetching the latest item, there will be no next link.
todo - gfm - 1/13/14 - is this still true?
Similarly, if you are fetching the first item, there will be no previous link.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://deihub/channel/stumptown/1000`

## fetch latest channel item

todo - gfm - 1/13/14 - only valid for sequence
To retrieve the latest item inserted into a channel, issue a HEAD request on the `latest` link returned from the channel
metadata.  The datahub will issue a 303 redirect.

`HEAD http://deihub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://deihub/channel/stumptown/00002FHSQESAS000`

Here is how you can do this with curl:

`curl -I http://deihub/channel/stumptown/latest`

## subscribe to events

todo - gfm - 1/13/14 - only valid for sequence
While a common approach to consuming data from the datahub involves traversing next/previous links, clients may 
"subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Once connected, the line-oriented protocol is simple:  
Each time data is inserted into the channel, the datahub will send a line to the client with the
URL for that content.

```
http://deihub/channel/stumptown/1000
http://deihub/channel/stumptown/1001
http://deihub/channel/stumptown/1002
...etc...
```

## time index
todo - gfm - 1/13/14 - time index interface
