---
title: FAQ
keywords: FAQ
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_delete.html
folder: hub
---


## Why does /latest (or /earliest) return 404?

Either data has never been added to that channel, or the last data added to that channel is older than the time to live (ttlDays).

## How can I guarantee ordering for items within a channel?

You can wait for the response for an item before writing the next item.  