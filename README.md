datahub
=======

The FlightStats Data Hub

The Data Hub is a robust platform for data distribution.  

There are some descriptions of this here:
http://confluence.office/display/TECH/Data+Hub

* [list channels](#list-channels)
* [create a channel](#create-a-channel)
* [update a channel](#update-a-channel)
* [fetch channel metadata](#fetch-channel-metadata)
* [insert content into channel](#insert-content-into-channel)
* [fetch content from channel](#fetch-content-from-channel)
* [fetch latest channel item](#fetch-latest-channel-item)
* [subscribe to events](#subscribe-to-events)

For the purposes of this document, the datahub is at http://datahub:8080.

In development, it is actually at: http://datahub.svc.dev.
In staging, it is actually at: http://datahub.svc.staging.

## list channels

To obtain the list of channels:

`GET http://datahub:8080/channel`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://localhost:8080/channel"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://localhost:8080/channel/stumptown"
    }, {
      "name" : "ptown",
      "href" : "http://localhost:8080/channel/ptown"
    } ]
  }
}
```
    
## create a channel

Channel names _are case sensitive_, are limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.
Hyphens and underscores are not allowed in channel names. Please try to break as many of these rules as possible
in order to challenge Jason's sanity.

TTL is optional and should be a positive number. If not specified, a default value (120 days) is used. If specified as null,
then the channel has no TTL.

`POST http://datahub:8080/channel`

* Content-type: application/json

```json
{
   "name": "stumptown"
   "ttlMillis": "3600000" //one hour in millis
}
```

On success:  `HTTP/1.1 200 OK`

```json
{
    "_links": {
        "self": {
            "href": "http://datahub:8080/channel/stumptown"
        },
        "latest": {
            "href": "http://datahub:8080/channel/stumptown/latest"
        },
        "ws": {
            "href": "ws://datahub:8080/channel/stumptown/ws"
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
    http://datahub:8080/channel
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format (currently, only TTL can be updated).

`PATCH http://datahub:8080/channel/channelname`

* Content-type: application/json

```json
{
   "ttlMillis": "30000" //30 seconds
}
```

On success:  `HTTP/1.1 200 OK`, and the new channel metadata is returned (see example return data from create channel).

Here's how you can do this with curl:
```bash
curl -i -X POST --header "Content-type: application/json" \
    --data '{"ttlMillis": "30000"}'  \
    http://datahub:8080/channel/stumptown
```

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://datahub:8080/channel/stumptown`

On success: `HTTP/1.1 200 OK`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://datahub:8080/channel/stumptown"
    },
    "latest" : {
      "href" : "http://datahub:8080/channel/stumptown/latest"
    },
    "ws" : {
      "href" : "ws://datahub:8080/channel/stumptown/ws"
    }
  },
  "lastUpdateDate" : "2013-04-23T20:36:35.310Z",
  "name" : "stumptown",
  "creationDate" : "2013-04-23T20:36:17.885Z"
}
```

Here's how you can do this with curl:

`curl http://datahub:8080/channel/stumptown`

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` and
`Content-Language` headers are optional:

```
POST http://datahub:8080/channel/stumptown
Content-type: text/plain
Content-Language: en
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://datahub:8080/channel/stumptown/00002FHOK8JMK000`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://datahub:8080/channel/stumptown"
    },
    "self" : {
      "href" : "http://datahub:8080/channel/stumptown/00002FHOK8JMK000"
    }
  },
  "id" : "00002FHOK8JMK000",
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:

```bash
curl -i -X POST --header "Content-type: text/plain" \
    --data "your content here" \
    http://datahub:8080/channel/stumptown
```

## fetch content from channel

To fetch content that was stored into a datahub channel, do a `GET` on the `self` link in the above response:

`GET http://datahub:8080/channel/stumptown/00002FHOK8JMK000`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://datahub:8080/channel/stumptown/00002FHK7LV40000>;rel="previous"
Link: <http://datahub:8080/channel/stumptown/00002FHSQESAS000>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
Also note that there are two `Link` headers that provide links to the previous and next items in the channel.
If you are fetching the latest item, there will be no next link.
Similarly, if you are fetching the first item, there will be no previous link.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://datahub:8080/channel/stumptown/00002FHOK8JMK000`

## fetch latest channel item

To retrieve the latest item inserted into a channel, issue a HEAD request on the `latest` link returned from the channel
metadata.  The datahub will issue a 303 redirect.

`HEAD http://datahub:8080/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://datahub:8080/channel/stumptown/00002FHSQESAS000`

Here is how you can do this with curl:

`curl -I http://datahub:8080/channel/stumptown/latest`

## subscribe to events

While a common approach to consuming data from the datahub involves traversing next/previous links, clients may 
"subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Once connected, the line-oriented protocol is simple:  
Each time data is inserted into the channel, the datahub will send a line to the client with the
URL for that content.

```
http://datahub:8080/channel/stumptown/00002FHSTSRHC000
http://datahub:8080/channel/stumptown/00002FHSTVFQE000
http://datahub:8080/channel/stumptown/00002FHSU7R3S000
...etc...
```
