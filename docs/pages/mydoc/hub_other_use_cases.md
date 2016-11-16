---
title: Use Cases
keywords: 
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_other_use_cases.html
folder: hub
---

## Sequential Use Case {#sequential}

This covers the scenario where items in a channel need to maintain a consistent order.  
To guarantee this, the client must write items one at a time, waiting for the successful 
response before writing the next item.  In the case of a failure response, the item should 
be retried before proceeding.

In this use case, the ordering of items in a channel will always be consistent through all 
interfaces, including Group Callback, Events, WebSocket, Next/Previous and Time APIs.

## Parallel Use Case

For cases where read ordering are not required to be exactly the same as write order 
(otherwise see Sequential Write Use Case), the requirement is that read order should be consistent. 
That is, once a consumer reads data from the channel, subsequent reads of the same items at a later 
date will be in the same order.

To do this, there needs to be a lag from now for the latest items in a channel. By default, 
this lag is five seconds.

{% include links.html %}