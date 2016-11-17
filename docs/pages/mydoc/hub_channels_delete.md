---
title: Deleting Channels
keywords: channel
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_delete.html
folder: hub
---

To delete a channel when after you no longer need it, simply issue a `DELETE` command to that channel.
Delete returns a 202, indicating that the request has been accepted, and will take an indeterminate time to process.
If you re-create a channel before all the data has been deleted, the behavior is undefined.

 `DELETE http://hub/channel/stumptown`

Here's how you can do this with curl:
```bash
curl -i -X DELETE http://hub/channel/stumptown
```

{% include links.html %}