---
title: Alerts
keywords: notifications, alerts
last_updated: July 3, 2016
tags: [notifications]
summary: 
sidebar: mydoc_sidebar
permalink: hub_notifications_alerts.html
folder: hub
---


The hub can send alerts based on the number of items in a channel, or how long a webhook is lagging a channel.

For channels, an alert is created if inserts in `source` `operator` `threshold` within `timeWindowMinutes`
eg: if inserts in stumptown <  100 within 20 minutes

For webhooks, an alert is created if the callback `source` lags behind it's channel by `timeWindowMinutes`
eg: if the last completed callback to stumptownCallback is 10 minutes behind the last insert into it's channel

* `name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.

* `source` is the name of the channel or webhook to monitor

* `serviceName` is a user defined end point for the alert, which could be an email address, service name, etc

* `type` can be `channel` or `webhook`

* `timeWindowMinutes` the period of time to evaluate

* `operator` (channel only) can be `>=`, `>`, `==`, `<`, or `<=`

* `threshold` (channel only) is the value to compare

## create or change and alert {#create}

Alerts can be created and changed with PUT 

`PUT http://hub/alert/stumptownAlert`

* Content-type: application/json

```json
{
    "source": "stumptown",
    "serviceName": "stumptown@example.com",
    "timeWindowMinutes": 5,
    "type": "channel",
    "operator": "==",
    "threshold": 0
}
```

On success:  `HTTP/1.1 201 OK`

```json
{
    "name": "stumptownAlert",
    "source": "stumptown",
    "serviceName": "stumptown@example.com",
    "timeWindowMinutes": 5,
    "type": "channel",
    "operator": "==",
    "threshold": 0,
    "_links": {
        "self": {
            "href": "http://hub/alert/stumptownAlert"
        },
        "status": {
            "href": "http://hub/alert/stumptownAlert/status"
        }
    }
}
```

## channel alert status {#status}

Following the status link from _links.status.href shows the channel history for the current state of the alert

`GET http://hub/alert/stumptownAlert/status`

```json
{
    "name": "stumptownAlert",
    "period": "minute",
    "alert": true,
    "type": "channel",
    "history": [
    {
        "href": "http://hub/channel/stumptown/2015/06/17/19/21?stable=true",
        "items": 0
    },
    {
        "href": "http://hub/channel/stumptown/2015/06/17/19/22?stable=true",
        "items": 0
    },
    {
        "href": "http://hub/channel/stumptown/2015/06/17/19/23?stable=true",
        "items": 0
    },
    {
        "href": "http://hub/channel/stumptown/2015/06/17/19/24?stable=true",
        "items": 0
    },
    {
        "href": "http://hub/channel/stumptown/2015/06/17/19/25?stable=true",
        "items": 0
    }
    ],
    "_links": {
        "self": {
            "href": "http://hub/alert/stumptownAlert/status"
        }
    }
}
```



{% include links.html %}