---
title: Inserting into channels
keywords: channel
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_insert.html
folder: hub
---


## insert individual payloads {#individual}

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` header is optional.

Clustered Hubs using AWS have a configurable max item size, the default is 390 GB.
A singleHub which only uses Spoke has a default max item size of 40 MB, which is [configurable](hub_install_locally.md)


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

## bulk insert content into channel {#bulk}

If your data source will be writing a lot of data per second - or have very large spikes of data in a short amount of time,
consider doing bulk inserts.  This is an optimized way of writing a lot of data that will put less load on the Hub.

When you insert items in bulk into the hub, each item will receive a unique ordered uri.

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

## provider interface {#provider}

For external data providers, there is a simplified interface suitable for exposing to the authenticated outside world.

`POST http://hub/provider/`

* it creates a channel if it doesn't exist
* it expects a `channelName` header
* does not support any other HTTP methods
* does not return any links
* access by external data providers is controlled through a proxy maintained by Operations


{% include links.html %}