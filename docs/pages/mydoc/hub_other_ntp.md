---
title: NTP
keywords: ntp
last_updated: July 3, 2016
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_other_ntp.html
folder: hub
---

In the Hub, we use time as part of key for each item.  To meet the [Sequential Write Use Case](hub_other_use_cases.html#sequential), it is imperative that the time between the hub nodes be closely aligned so that sequential inserts are written and represented in sequential write time order.

## Changing Settings

When changing ntp settings on a node, it is expected that ntp will take some period of time to achieve a stable state.  To prevent the unstable period from having an impact on hub operations, a node can be told to use other servers in the cluster for time sensitive operations.

To force a node to use other servers:

```
curl -i -X PUT http://hub-node:8080/internal/time/remote
```

To reset a node once it's time is stable

```
curl -i -X PUT http://hub-node:8080/internal/time/local
```

## Suggested Settings

These settings can keep hub nodes within a 1ms of each other (not necessarily the ntp server).
This setup requires that you have an ntp server on the same network for low latency, named `close-ntp-server` below.

Edit /etc/ntp.conf
------------------
Edit ntp.conf to make the nodes peers.

config for hub-01

```
server close-ntp-server iburst prefer maxpoll 6
peer hub-02 iburst maxpoll 6
peer hub-03 iburst maxpoll 6
```

```
$ sudo service ntp restart
 * Stopping NTP server ntpd                                              [ OK ]
 * Starting NTP server ntpd                                              [ OK ]
$ ntpq -p
     remote           refid      st t when poll reach   delay   offset  jitter
==============================================================================
+close-ntp-serve 192.168.0.1       3 u   24  128  377  107.426    0.288   0.396
+hub-02          192.168.0.2       5 u   77  128  376    0.270    0.103   0.106
-hub-03          192.168.0.2       5 u   67  128  276    0.279   -0.435   0.781
```

## Hard Reset
If an instance is very far off from the other servers:

```
sudo service hub stop
sudo service ntp stop
sudo ntpdate -s 192.168.0.1
sudo rm /var/lib/ntp/ntp.drift
sudo service ntp start
```
Monitor the divergence before starting the hub.
[NTPMonitor](https://github.com/flightstats/hub/blob/master/src/main/java/com/flightstats/hub/time/NTPMonitor.java) calculates and reports the delta between all of the nodes in a cluster.

{% include links.html %}