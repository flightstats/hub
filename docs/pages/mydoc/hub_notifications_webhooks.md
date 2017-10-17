---
title: Webhooks
keywords: channel, notification, webhook
last_updated: July 3, 2016
tags: [channel, notification]
summary: 
sidebar: mydoc_sidebar
permalink: hub_notifications_webhooks.html
folder: hub
---

A Webhook is registered for a client's http endpoint and that endpoint recieves Http POSTs of json uris, and the Hub server keeps track of the Webhook's state.

* `name` is used in the url for the callback.  Names are limited to 48 characters and may only contain `a-z`, `A-Z`, `0-9`, hyphen `-` and underscore `_`.

* `callbackUrl` is the fully qualified location to receive callbacks from the server.

* `channelUrl` is the fully qualified channel location to monitor for new items.  This needs to be the url you use to access the hub.
The url's scheme and host may be changed, but the name of the channel can't change.

* `parallelCalls` is the optional number of callbacks to make in parallel.  The default value is `1`.
If parallelCalls is higher than one, callback ordering is not guaranteed.
parallelCalls can be modified with a call to PUT 

* `startItem` is the optional location where the callback should start from.
  If startItem is a fully qualified item, that next item after it will be sent via the callback.
  If startItem is 'previous', the previous stable item on the channel will be sent as the first callback item.
  startItem is *only* used when creating a webhook.  If you want to change the pointer of a callback, you will need to
delete the callback first.

* `paused` is optional and defaults to false.   When true, this will pause a webhook.

* `batch` is optional and defaults to `SINGLE`, which will return each item by itself.
  Setting the value to `SECOND` will return each second's worth of data in the channel.
  Setting the value to `MINUTE` will return each minute's worth of data in the channel.
  SECOND and MINUTE callbacks will return an empty array of uris if there are no items.

* `heartbeat` is optional and defaults to false for `SINGLE`. `MINUTE` batches always have a heartbeat.
   A heartbeat is a callback which identifies the end of a minute period.  It may have an empty `uris` array.
   It will include an `id` field which identifies the ending minute.
   
* `maxWaitMinutes` is optional and defaults to 1.  maxWaitMinutes is the maximum amount of time between retry attempts to the callbackUrl.

* `ttlMinutes` is optional and defaults to 0.  If ttlMinutes is greater than 0, the hub will not attempt to send an item which is older than the ttl.

## List existing webhooks {#list}

`GET http://hub/webhook`
 
## Create new webhook {#new}

`PUT http://hub/webhook/{name}`

``` json
{
  "callbackUrl" : "http://client/path/callback",
  "channelUrl" : "http://hub/channel/stumptown",
  "parallelCalls" : 2,
  "startItem" : "http://hub/channel/stumptown/2015/02/06/22/28/43/239/s03ub2",
  "paused" : false,
  "batch" : "SINGLE",
  "heartbeat" : false,
  "maxWaitMinutes" : 1,
  "ttlMinutes" : 0
}
```

Once a Webhook is created, the channel's name can not change.  PUT may be safely called multiple times with the same
 configuration.  Changes to `batch` will be ignored.  Change to `startItem` will update the cursor to the startItem key.

To see the configuration and status of a webhook:

`GET http://hub/webhook/{name}`

## Delete a webhook {#delete}

`DELETE http://hub/webhook/{name}`

DELETE will return a 202, and it may take up to a minute to properly stop a webhook from servicing the callback.

## Update a webhook cursor
With this api, you can adjust the webhook cursor forward or backward in time. 
 
`PUT http://hub/webhook/{name}/updateCursor`

With a channel url as a body.  e.g.:

`http://localhost:8080/channel/coffee/2017/07/17/07/07/07`


The same result can be accomplished with updating the startItem and re-PUTting (upserting) the webhook.

#####HTTPie example
```
http PUT localhost:8080/webhook/coffeeWebhook/updateCursor \
        Content-Type:text/plain \
        http://localhost:8080/channel/coffee/2017/07/17/07/07/07
```
## Webhook behavior

The application listening at `callbackUrl` will get a payload POSTed to it for every new item in the channel, starting after `startItem` or at the time the webhook is created.
The POST has a `Content-type` of `application/json`.
A 2XX-level client response is considered successful.  Any other response is considered an error, and will cause the server to retry.   Redirects are allowed.
Retries will use an exponential backoff up to one minute, and the server will continue to retry at one minute intervals indefinitely.

An example SINGLE payload:

``` json
{
  "name" : "stumptownCallback",
  "type" : "item",
  "uris" : [ "http://hub/channel/stumptown/2014/01/13/10/42/31/759/s03ub2" ]
}
```

An example SINGLE heartbeat:

``` json
{
  "name" : "stumptownCallback",
  "type" : "heartbeat",
  "id" : "2014/01/13/10/42",
  "uris" : []
}
```

An example MINUTE payload:

``` json
{
  "name" : "stumptownCallbackBatch",
  "type" : "items",
  "id" : "2014/01/13/10/42",
  "url" : "http://hub/channel/stumptown/2014/01/13/10/42",
  "bulkUrl" : "http://hub/channel/stumptown/2014/01/13/10/42?bulk=true",
  "uris" : [ 
    "http://hub/channel/stumptown/2014/01/13/10/42/05/436/abcdef",
    "http://hub/channel/stumptown/2014/01/13/10/42/31/759/s03ub2"
    "http://hub/channel/stumptown/2014/01/13/10/42/39/029/zxcvbn"
  ]
}
```

An example MINUTE heartbeat :

``` json
{
  "name" : "stumptownCallbackBatch",
  "type" : "heartbeat",
  "id" : "2014/01/13/10/42",
  "url" : "http://hub/channel/stumptown/2014/01/13/10/42",
  "bulkUrl" : "http://hub/channel/stumptown/2014/01/13/10/42?bulk=true",
  "uris" : [ ]
}
```

## Tag webhooks

Tag Webhooks are a way to automate the creation and deletion of webhooks for channels that have a specific tag.  It is useful if you have several channels that emit to the same webhook client.  If a Tag Webhook is defined, all channels containing a the specified tag, will have a webhook automatically created based on the Tag Webhook "prototype" configuration.  Removing a tag from the channel will also remove the associated Tag Webhook instance associated with that channel.  Likewise, adding a tag to a new or existing channel will create an associated webhook if the tag has a Tag Webhook defined for it.

#### Creating a Tag Webhook 

You create a Tag Webhook the same way as you would create a normal Webhook with two configuration exceptions:

* `tagUrl`: add fully qualified tag url (e.g. `http://hub.com/tag/{tagName}`)
* `channelUrl`: Don't include a channelUrl.  Webhooks will accept either a tagUrl (for tag webhooks) or a channelUrl (for normal webhooks) but not both.


#### Deleting a Tag Webhook

You delete a tag webhook as you would a normal webhook, but note that when you delete a tag webhook, all actual "instances" of the Tag Webhook will be automatically deleted as well.
 

{% include links.html %}
