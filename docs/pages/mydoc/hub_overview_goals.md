---
title: Design Goals
keywords: 
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_overview_goals.html
folder: hub
---

# Hub Design Goals

* Easy Access To Data
    * Simple Protocol
    * Replay and Auditing
* Data Agnostic
* Modular Design
* Fault Tolerance
* Loose Coupling
* Distributed Parallel Clients

## Easy Access To Data

We want to make sharing data between teams as easy as possible.
* The data a hub contains is easily discovered and consumed
* Channels are used to separate data from different sources
* Tags are used to group multiple related channels together

### Simple Protocol

* All interactions with the hub use HTTP
* Clients can be written in any language   
* We can leverage existing expertise with proxies, load balancers and authentication 

### Replay and Auditing

The Hub can answer the following questions about a channel:
* What is the oldest item?
* What is the most recent item?
* What items where written during a time period (day, hour, minute, second)?
* What are the next or previous items relative to a specific time or item?

All data in the hub is immutable and indexed by time with [consistent ordering](Sequential-Write-Use-Case)

## Data Agnostic

Users can store any type of data in the hub, from plain text to binary files.
[Content-type](https://github.com/flightstats/hub#insert-content-into-channel) is used by data producers to indicate data type.

## Modular Design
The simple queries mean all questions can be answered by using the union of all available data sources.
todo talk about Spoke and both S3 versions, as well as the local version.

The modular design has allowed us to more easily add additional features. 
 
* [Historical-Channel](Historical-Channel)

## Loose Coupling
## Fault Tolerance
## Distributed Parallel Clients


{% include links.html %}