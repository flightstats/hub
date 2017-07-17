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

Clients may "subscribe" to single channel's items by listening on a websocket. **Clients should be aware that websockets are a "best effort" service.**

## Available Endpoints

### Channel
Returns items after the latest item.
```
ws://hub/channel/{channel}/ws
```

### Time
Returns items starting at the provided time.
```
ws://hub/channel/{channel}/{Y}/{M}/{D}/{h}/ws
ws://hub/channel/{channel}/{Y}/{M}/{D}/{h}/{m}/ws
ws://hub/channel/{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/ws
```

### Item
Returns items after the provided item.
```
ws://hub/channel/{channel}/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/ws
```

## How to interpret the response
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