---
title: Replication
keywords: channel, replication
last_updated: July 3, 2016
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_replication.html
folder: hub
---


The hub can replicate a source channel from another hub instance into a destination channel.  The destination channel can have any name.

To configure replication, specify `replicationSource` when creating the new channel in the desired destination.

To stop replication, either delete the destination channel, or PUT the destination channel with a blank `replicationSource`.

Modifications to configuration takes effect immediately.

Replication destination channels do not allow inserts.

{% include links.html %}