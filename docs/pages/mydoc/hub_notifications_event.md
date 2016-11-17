---
title: Events
keywords: channel, notification, events
last_updated: July 3, 2016
tags: [channel, notification]
summary: 
sidebar: mydoc_sidebar
permalink: hub_notifications_events.html
folder: hub
---

Clients connecting to an event endpoint will receive the id, content type and payload of each new item in the channel.
The [Server Sent Events](#https://www.w3.org/TR/eventsource/) standard defines the http interface and format. 
The format is designed for UTF-8 payloads.

Calling `curl http://hub/channel/stumptown/events` will return every new item in chronological order.
Some browsers support events natively also.
 
```

event: application/json
id: http://hub/channel/stumptown/2014/01/13/10/42/31/149/QWERTY
data: {"order": 474689, "item": "latte"}

event: application/json
id: http://hub/channel/stumptown/2014/01/13/10/42/31/201/ASDFGH
data: {"order": 474690, "item": "americano"}

event: application/json
id: http://hub/channel/stumptown/2014/01/13/10/42/31/251/ZXCVBN
data: {"order": 474691, "item": "drip"}

...etc...
```

Events can also be started from an item `http://hub/channel/stumptown/2014/01/13/10/42/31/149/QWERTY/events`

Events also supports the `Last-Event-ID` header.  Some clients will automatically attempt to reconnect, and when they reconnect,
the client will include the last `id` received as the `Last-Event-ID` header.  The header can be used on either endpoint.

The following examples will both start at the same point in the channel:

```
curl -i --header "Last-Event-ID: http://hub/channel/stumptown/2014/01/13/10/42/31/201/ASDFGH" http://localhost:8080/channel/streamTest/events

curl -i --header "Last-Event-ID: http://hub/channel/stumptown/2014/01/13/10/42/31/201/ASDFGH" http://hub/channel/stumptown/2014/01/13/10/42/31/149/QWERTY/events

```


{% include links.html %}