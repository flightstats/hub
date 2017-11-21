---
title: Protected Channels
keywords: channel, protect, access control
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_protect.html
folder: hub
---

While the hub's API prioritizes data access and ease of manipulation, there are a number of scenarios where we want 
to prevent data from being lost, such as accidentally changing ttlDays from 1000 to 1.
  
Every hub channel has a 'protect' attribute.  
If protect is false, any user can change any setting of a channel, which is useful in dev and staging environments. 

If `protect` is true:
* `protect` can not be changed
* `ttlDays` and `maxItems` can not decrease
* `tags` can not be removed
* `owner` can not change 
* `replicationSource` can not change
* `storage` can only be changed to `BOTH`
* channel can not be deleted

Instead, the changes need to be made from the localhost of a hub server in the cluster.
 
```
curl -i -X PUT --header "Content-type: application/json"  --data '{"ttlDays" : 1}' http://localhost:8080/channel/stumptown
```

Since protected channels are desired in our production environments, we can force all channels to be protected by
setting the hub property `hub.protect.channels` to `true`.
If `hub.protect.channels` is `false`, end users can optionally set `protect` on specific channels.

{% include links.html %}