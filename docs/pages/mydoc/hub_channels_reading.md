---
title: Reading data from channels
keywords: channel, create, update
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_reading.html
folder: hub
---


## fetch content from channel {#specific}

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

## fetch latest channel item {#latest}

To retrieve the latest item inserted into a channel, issue a HEAD or GET request on the `latest` link
returned from the channel metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`

`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

Here is how you can do this with curl:

`curl -I http://hub/channel/stumptown/latest`

You can also retrieve the latest N items by using /latest/{n}

## fetch earliest channel item {#earliest}

To retrieve the earliest item inserted into a channel, issue a HEAD or GET request on the `earliest` link
returned from the channel metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub/channel/stumptown/earliest`

On success:  `HTTP/1.1 303 See Other`

`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

Here is how you can do this with curl:

`curl -I http://hub/channel/stumptown/earliest`

You can also retrieve the earliest N items by using /earliest/{n}

## next and previous links {#next-and-previous}

Any item's uri can be appended with /next or /previous to navigate forward or backward.  A 404 will be returned if there isn't an item.
The starting point does not need to be an real item in the hub, it can be a time.   

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

## fetch bulk content from channel {#bulk}

Any query operation (including next, previous, earliest, latest, and times) supports the addition of the
query parameter `?bulk=true`.  Using the bulk parameter will result in the content of the query items being streamed
as [MIME](https://tools.ietf.org/html/rfc2045) or as a zip file to the client.
To get a zip file, specify the 'Accept' header as `application/zip`.

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

{% include links.html %}