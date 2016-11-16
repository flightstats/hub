---
title: Websockets
keywords: channel
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_notifications_websocket.html
folder: hub
---

Clients may "subscribe" to single channel's items by listening on a websocket.  
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

{% include links.html %}