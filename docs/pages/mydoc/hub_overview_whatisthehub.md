---
title: What is the hub
keywords: overview
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_overview_whatisthehub.html
folder: hub
---

{% include image.html file="6-blind-men-hans.jpg" url="" alt="" caption="Blind Men and the Elephant" %}

The hub is a distributed linked list.   
The hub is a messaging system.   
The hub is a long term data store.   

## The hub is like a Key Value Store 
* Stores each item at a unique key (url) (http://hub/channel/whatIs/2016/12/31/23/59/59/bhJ23I)
* Items are available for an arbitrarily long time
* Fault tolerant, easy to cluster
* Requires a quorum for a successful write 

###  with some differences 
* Most KV stores allow mutations.  The hub does not.
* The hub imposes that all item keys always move forward in time
* The hub offers ordering guarantees to provide consistent answers to time based queries
 
## The hub is like a Messaging system
* Items are immutable
* Item keys always increase

### and some differences
* Most messaging systems do not let you access arbitrarily old items
* Many messaging systems are difficult to cluster
* Many messaging system require custom clients


{% include links.html %}
