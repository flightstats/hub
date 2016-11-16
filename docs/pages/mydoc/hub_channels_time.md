---
title: Channel Time Interface
keywords: channel
last_updated: July 3, 2016
tags: [channel time api]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_time.html
folder: hub
---

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
}
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items inserted within the minute so far, and will
increase as other items are inserted.

# Time resolution

You can request all of the items by the time resolution you specify in the URL.  
For all the items in a minute: `GET http://hub/channel/stumptown/2014/01/13/10/42`
For all the items in an hour: `GET http://hub/channel/stumptown/2014/01/13/10`

You can also access the urls via convenience methods:

`HEAD http://hub/channel/stumptown/time/minute`
`HEAD http://hub/channel/stumptown/time/hour`

The output format is the same regardless of time resolution


{% include links.html %}