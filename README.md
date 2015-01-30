The Hub V2
=======

* [overview](#overview)
* [consistency](#consistency)
* [new features in v2](#new-features-in-v2)
* [clients](#clients)
* [error handling](#error-handling)
* [FAQ](#faq)
* [hub resources](#hub-resources)
* [list channels](#list-channels)
* [create a channel](#create-a-channel)
* [update a channel](#update-a-channel)
* [fetch channel metadata](#fetch-channel-metadata)
* [insert content into channel](#insert-content-into-channel)
* [fetch content from channel](#fetch-content-from-channel)
* [latest channel item](#latest-channel-item)
* [next and previous links](#next-and-previous-links)
* [channel status](#channel-status)
* [tag interface](#tag-interface)
* [time interface](#time-interface)
* [subscribe to events](#subscribe-to-events)
* [group callback interface](#group-callback-interface)
* [provider interface](#provider-interface)
* [delete a channel](#delete-a-channel)
* [replication](#replication)
* [health check](#health-check)
* [access control](#access-control)
* [encrypted-hub](#encrypted-hub)
* [api changes from v1 to v2](#api-changes-from-v1-to-v2)
* [monitoring](#monitoring)
* [development](#development)
* [deployments](#deployments)
* [Requirements Notes](#Requirements-Notes)

For the purposes of this document, the Hub is at http://hub-v2/.

* On your local machine it is at: http://localhost:9080/
* In development: http://hub-v2/
* In staging: http://hub-v2.svc.staging/
* In production: http://hub-v2.svc.prod/ (coming soon!)

## overview

The Hub is designed to be a fault tolerant, highly available service for data storage and distribution.  Most features are available via a REST API.

It currently only supports channels ordered by time.  Note: The sequence channel API is deprecated and will be supported separately as [Hub V1](https://github.com/flightstats/hub).

Channels represent uniquely addressable items that are iterable and query-able by time.  Each item may be up to to 20 MB.

The [encrypted-hub](#encrypted-hub) (EH) is a separate installation of the Hub.
The features and API of the EH are mostly the same as the Hub, with a few additions.

## new features in v2

* [next and previous links](#next-and-previous-links) will let you request a batch of items.
* The [time interface](#time-interface) is completely new for v2.  Previously users could only get items by the minute. Now you can query by second, minute, hour or day.
* Channels can be idempotently PUT with [create a channel](#create-a-channel).
* Fixed - 62 second 404 delay for missing items.  404s will return quickly.
* Performance - PUTs and GETs for channel items are significantly faster.  Typical times are ~10ms.
* [replication](#replication) is now a channel level setting.  Replicated channels can have new names.

## consistency

* All times from the Hub are in UTC.
* By default all iteration, queries, group callbacks and websockets return items with stable ordering.  Data is considered stable when iteration will provide consistent results.
* All requests for a specific item by id will return that item if it exists.

## clients

Since the Hub provides an http interface, most tools which support http can access the Hub.
Examples on this page are in curl, most GETs will work in a browser.

In addition, some more sophisticated clients exist:
* [Java](https://github.com/flightstats/datahub-client/)
* [Javascript](https://github.com/flightstats/edge-lib/tree/master/datahub)
* [Utilities](https://github.com/flightstats/hub-utilities)
* [Zombo](https://github.com/flightstats/zombo)

## error handling

Clients should consider handling transient server errors (500 level return codes) with retry logic.  This helps to ensure that transient issues (networking, etc)
  do not prevent the client from entering data. For Java clients, this framework provides many options - https://github.com/rholder/guava-retrying
The DDT team recommends clients use exponential backoff.

## FAQ

* Why does /latest return 404?

  Either data has never been added to that channel, or the last data added to that channel is older than the time to live (ttlDays).

* How can I guarantee ordering for items within a channel?

  You can wait for the response for an item before writing the next item.  

## hub resources

To explore the Resources available in the Hub, go to http://hub-v2/

## list channels

To obtain the list of channels:

`GET http://hub-v2/channel`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub-v2/channel"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://hub-v2/channel/stumptown"
    }, {
      "name" : "ptown",
      "href" : "http://hub-v2/channel/ptown"
    } ]
  }
}
```
    
## create a channel

`name` _is case sensitive_, is limited to _48 characters_, and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.
Hyphens are not allowed in channel names. Surrounding white space is trimmed (e.g. "  foo  " -> "foo" ).
Channels starting with `test` will automatically be deleted in the dev and staging environments every hour using [Jenkins](http://ops-jenkins01.cloud-east.dev/job/hub-cleanup-hourly/)

`ttlDays` is optional and should be a positive number. If not specified, a default value of 120 days is used.

`description` is optional and defaults to an empty string.  This text field can be up to 1024 bytes long.

`tags` is an optional array of string values.  Tag values are limited to 48 characters, and may only contain `a-z`, `A-Z` and `0-9`.
A channel may have at most 20 tags.

'replicationSource' is the optional fully qualified path to a another hub channel.  The data from the other channel
will be duplicated into this channel.  Please see [replication](#replication) for more details.

**V2 Note**:
Channel items type, ttlMillis, contentSizeKB and peakRequestRateSeconds from V1 are no longer provided.
While PUT is shown here, the V1 POST to http://hub-v2/channel/ is still supported.

`PUT http://hub-v2/channel/stumptown`

* Content-type: application/json

```json
{
   "ttlDays": "14",
   "description": "a sequence of all the coffee orders from stumptown",
   "tags": ["coffee"]
}
```

On success:  `HTTP/1.1 201 OK`

```json
{
    "_links": {
        "self": {
            "href": "http://hub-v2/channel/stumptown"
        },
        "latest": {
            "href": "http://hub-v2/channel/stumptown/latest"
        },
        "ws": {
            "href": "ws://hub/channel/stumptown/ws"
        },
        "time": {
            "href": "http://hub-v2/channel/stumptown/time"
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlDays": 14,
    "description": "a sequence of all the coffee orders from stumptown",
    "tags": ["coffee"],
    "replicationSource": ""
}
```

Here's how you can do this with curl:
```bash
curl -i -X PUT http://hub-v2/channel/stumptown

or

curl -i -X PUT --header "Content-type: application/json"  --data '{ "description" : "stumpy", "ttlDays" : 1 }' http://localhost:9080/channel/stumptown
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format
(currently, only `ttlDays`, `description`, `tags` and `replicationSource` can be updated).
Each of these fields is optional.

**V2 Note**:
While PUT is shown here, the V1 PATCH to http://hub-v2/channel/channelname is still supported.

`PUT http://hub-v2/channel/channelname`


## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://hub-v2/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://hub-v2/channel/stumptown`

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` header is optional:

```
POST http://hub-v2/channel/stumptown
Content-type: text/plain
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://hub-v2/channel/stumptown"
    },
    "self" : {
      "href" : "http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}"
    }
  },
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:

```bash
curl -i -X POST --header "Content-type: text/plain" --data 'your content here' http://hub-v2/channel/stumptown
```

## fetch content from channel

To fetch content that was stored into a hub channel, do a `GET` on the `self` link in the above response:

`GET http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}/previous>;rel="previous"
Link: <http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}/next>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
There are two `Link` headers that provide links to the previous and next items in the channel.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

## fetch latest channel item

To retrieve the latest item inserted into a channel, issue a HEAD or GET request on the `latest` link 
returned from the channel metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub-v2/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub-v2/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

Here is how you can do this with curl:

`curl -I http://hub-v2/channel/stumptown/latest`

**New for v2** You can also retrieve the latest N items by using /latest/{n}

## next and previous links

A GET on the next and previous links returned as headers with content will redirect to those respective items.  A 404 will be returned if they don't exist.

**New for v2**, any item's uri can be appended with /next or /previous to navigate forward to backward.
If you append a number /next/20 or /previous/15, and you'll receive a list of that many items.

For example:

`GET http://hub-v2/channel/stumptown/2014/12/23/23/14/50/514/xIXX5L/previous/10`

On success: `HTTP/1.1 200 OK`
```
{
  "_links" : {
    "self" : {
      "href" : "http://hub-v2/channel/stumptown/2014/12/23/23/14/50/514/xIXX5L/previous/10"
    },
    "next" : {
      "href" : "http://hub-v2/channel/stumptown/2014/12/23/23/14/49/887/x46z8p/next/10"
    },
    "previous" : {
      "href" : "http://hub-v2/channel/stumptown/2014/12/23/23/14/42/751/mRklXw/previous/10"
    },
    "uris" : [
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/42/751/mRklXw", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/43/339/CJ9mt9", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/44/163/LzhylF", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/44/588/zDygpg", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/45/105/ZuJUmM", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/45/972/qeKDF6", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/46/703/Jm09Un", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/47/376/swdWJD", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/48/115/lDCHYY", 
        "http://hub-v2/channel/stumptown/2014/12/23/23/14/49/887/x46z8p" 
        ]
  }
}
```

## channel status

A GET on the status link for a channel will return the link to the latest item in the channel.
If a replicationSource is defined, it will also return the link the the latest in the replication hub.

`GET http://hub-v2/channel/stumptown/status`

On success: `HTTP/1.1 200 OK`
```
{
    "_links": {
        "self": {
            "href": "http://hub-v2/channel/stumptown/status"
        },
        "latest": {
            "href": "http://hub-v2/channel/stumptown/2015/01/23/17/33/08/895/1524"
        },
        "replicationSourceLatest": {
            "href": "http://hub-other/channel/stumptown/1526"
        }
    }
}
```

## tag interface

To retrieve all of the tags in the Hub:

`GET http://hub-v2/tag`

On success: `HTTP/1.1 200 OK`
```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub-v2/tag"
    },
    "tags" : [ {
      "name" : "orders",
      "href" : "http://hub-v2/tag/orders"
    }, {
      "name" : "coffee",
      "href" : "http://hub-v2/tag/coffee"
    } ]
  }
}
```

Any of the returned tag links can be followed to see all of the channels with that tag:

`GET http://hub-v2/tag/coffee`

On success: `HTTP/1.1 200 OK`
```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub-v2/tag/coffee"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://hub-v2/channel/stumptown"
    }, {
      "name" : "spella",
      "href" : "http://hub-v2/channel/spella"
    } ]
  }
}
```

## time interface

The time interface provides a variety of options to query data in a hub channel.  All queries will only show items
 with stable ordering by default.  If you want to see items which might be unstable, add the parameter ```?stable=false```

To see time format options, issue a GET request on the `time` link returned from the channel metadata.

`GET http://hub-v2/channel/stumptown/time`

```json
{
    "_links": {
        "self": {
            "href": "http://hub-v2/channel/stumptown/time"
        },
        "second": {
            "href": "http://hub-v2/channel/stumptown/2014/12/23/05/58/55",
            "template": "http://hub-v2/channel/stumptown/time/{year}/{month}/{day}/{hour}/{minute}/{second}{?stable}",
            "redirect": "http://hub-v2/channel/stumptown/time/second"
        },
        "minute": {
            "href": "http://hub-v2/channel/stumptown/2014/12/23/05/58",
            "template": "http://hub-v2/channel/stumptown/time/{year}/{month}/{day}/{hour}/{minute}{?stable}",
            "redirect": "http://hub-v2/channel/stumptown/time/minute"
        },
        "hour": {
            "href": "http://hub-v2/channel/stumptown/2014/12/23/05",
            "template": "http://hub-v2/channel/stumptown/time/{year}/{month}/{day}/{hour}{?stable}",
            "redirect": "http://hub-v2/channel/stumptown/time/hour"
        },
        "day": {
            "href": "http://hub-v2/channel/stumptown/2014/12/23",
            "template": "http://hub-v2/channel/stumptown/time/{year}/{month}/{day}{?stable}",
            "redirect": "http://hub-v2/channel/stumptown/time/day"
        }
    },
    "now": {
        "iso8601": "2014-12-23T05:59:00.162Z",
        "millis": 1419314340162
    },
    "stable": {
        "iso8601": "2014-12-23T05:58:55.000Z",
        "millis": 1419314335000
    }
}
```

Call one of redirect uris, and the Hub will issue a 303 redirect for the current time with the specified resolution.

`HEAD http://localhost:9080/channel/stumptown/time/second`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub-v2/channel/stumptown/2014/01/13/10/42/31

A GET on the returned URI will return all of the content URIs within that period.

`GET http://hub-v2/channel/stumptown/2014/01/13/10/42/31`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub-v2/channel/stumptown/2014/01/13/10/42/31"
    },
    "uris" : [ 
        "http://hub-v2/channel/stumptown/2014/01/13/10/42/31/149/{hash1}", 
        "http://hub-v2/channel/stumptown/2014/01/13/10/42/31/359/{hash2}",
        "http://hub-v2/channel/stumptown/2014/01/13/10/42/31/642/{hash3}" 
    ]
  }
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items inserted within the minute so far, and will
change as other items are inserted.

### time resolution

You can request all of the items by the time resolution you specify in the URL.  
For all the items in a minute: `GET http://hub-v2/channel/stumptown/2014/01/13/10/42`
For all the items in an hour: `GET http://hub-v2/channel/stumptown/2014/01/13/10`

You can also access the urls via convenience methods:

`HEAD http://hub-v2/channel/stumptown/time/minute`
`HEAD http://hub-v2/channel/stumptown/time/hour`

The output format is the same regardless of time resolution

## subscribe to events

Clients may "subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Clients should be aware that websockets are a "best effort" service, and are not stateful.  If the ws connection is lost,
which will happen on hub server restarts, the client will need to reconnect, and may have missed items.

Once connected, the line-oriented protocol is simple:

Each time data is inserted into the channel, the hub will send a line to the client with the
URL for that content.

```
http://hub-v2/channel/stumptown/2014/01/13/10/42/31/149/{hash1}
http://hub-v2/channel/stumptown/2014/01/13/10/42/31/359/{hash2}
http://hub-v2/channel/stumptown/2014/01/13/10/42/31/642/{hash3}
...etc...
```

## group callback interface

The Group Callback mechanism is an alternative to WebSockets for consuming events.  These push notifications use HTTP, and 
the Hub server keeps track of the Group's state.

`name` is used in the url for the callback.  Names are limited to 48 characters and may only contain `a-z`, `A-Z`, `0-9` and underscore `_`.
`callbackUrl` is the fully qualified location to receive callbacks from the server.  
`channelUrl` is the fully qualified channel location to monitor.  
`parallelCalls` is the optional number of callbacks to make in parallel.  The default value is `1`.  
If parallelCalls is higher than one, callback ordering is not guaranteed.

To get a list of existing group callbacks:

`GET http://hub-v2/group`
 
To create a new group callback:

`PUT http://hub-v2/group/{name}`

``` json
{
  "callbackUrl" : "http://client/path/callback",
  "channelUrl" : "http://hub-v2/channel/stumptown",
  "parallelCalls" : 2
}
```

Once a Group is created, it can not be changed, only deleted.  Put may be safely called multiple times with the same 
 configuration.

To see the configuration and status of a group callback:

`GET http://hub-v2/group/{name}`

To delete a group callback:

`DELETE http://hub-v2/group/{name}`

Delete will return a 202, and it may take up to a minute to properly stop a group from servicing the callback.

#### Behavior

The group listening to the `callbackUrl` will get a payload POSTed to it for every new item in the channel, starting at the time the group is created.  
200 is considered a successful response.  Any other response is considered an error, and will cause the server to retry.   Redirects are allowed.                                        
Retries will use an exponential backoff up to one minute, and the server will continue to retry at one minute intervals indefinitely.

``` json
{
  "name" : "stumptownCallback",
  "uris" : [ "http://hub-v2/channel/stumptown/2014/01/13/10/42/31/759/{hash1}" ]
}
```

If a client is running in Sungard, latency may limit throughput to ~3 per second.

## provider interface

For external data providers, there is a simplified interface suitable for exposing to the authenticated outside world.

`POST http://hub-v2/provider/`

* it creates a channel if it doesn't exist
* it expects a `channelName` header
* does not support any other HTTP methods
* does not return any links
* access by external data providers is controlled through a proxy maintained by Operations

## delete a channel


To delete a channel when after you no longer need it, simply issue a `DELETE` command to that channel.
Delete returns a 202, indicating that the request has been accepted, and will take an indeterminate time to process.
If you re-create a channel before all the data has been deleted, the behavior is undefined.

 `DELETE http://hub-v2/channel/stumptown`

Here's how you can do this with curl:
```bash
curl -i -X DELETE http://hub-v2/channel/stumptown
```

## replication

The v2 hub can replicate a source channel from another hub instance into a destination channel.  The destination channel can have any name.

To configure replication, specify `replicationSource` when creating the new channel in the desired destination.

To stop replication, either delete the destination channel, or PUT the destination channel with a blank `replicationSource`.

Modifications to configuration takes effect immediately.

Replication destination channels do not allow inserts.

### v1 to v2 replication

When replicating from hub v1 to hub v2, the location urls will change from v1 sequences to v2 time formats.
The creation time of the original item will stay the same.

## health check

The Health Check returns a 200 status code when the server can connect to each data store.
If the server can not access a data store, it will return a 500 status code.

Responding to http connections is the last step on startup, so the health check will be unresponsive until startup is complete.
On shutdown, the server immediately stops responding to new http connections, so there are no separate codes for startup and shutdown.

`GET http://hub-v2/health`

```json
{
  "healthy" : true,
  "description" : "OK",
  "version" : "2014-03-26.126"
}
```

## access control

Currently, all access to the Hub is uncontrolled, and does not require authentication.
Over time, access control will be added to some of the API, starting with Channel Deletion and Replication Management.
To request a change to a controlled API, or to request access, please use the [hub-discuss forum](https://groups.google.com/a/conducivetech.com/forum/#!forum/hub-discuss)

## encrypted-hub

**This is not yet implemented in V2**

The Encrypted Hub (EH) is a separate installation of the Hub.
The features and API of the EH are nearly identical, with a few additions.
EH also has some additional features to the normal Hub:

* All channel items are encrypted at rest
* All channel items are encrypted in flight (soon!)
* All access to channel items (reads and writes) require authentication and are access controlled (soon!)
* All access to channel items is audited

Channel inserts can be audited by a GET or HEAD to each channel item.  The creator of the record is returned in a `User` header.

All Channel reads are logged to a new channel <channel>_audit.   The auditing channel is automatically created for each channel in the EH.
Since it is a channel, you can perform the standard operations on it, however clients are not allowed to create or delete auditing channels.
All audit channels have an `audit` tag, which can not be modified by clients.

`GET http://hub-v2/channel/stumptown_encrypted_audit/1007`

```json
{
  "user": "somebody",
  "uri": "http://hub-v2/channel/stumptown_encrypted/1005",
  "date": "2014-05-22T20:56:08.739Z"
}
```

The EH is available at:

* In development: http://encrypted-hub-v2.svc.dev/ (soon!)
* In staging: http://encrypted-hub-v2.svc.staging/ (soon!)
* In production: http://encrypted-hub-v2.svc.prod/ (soon!)

## api changes from v1 to v2

Channel items type, ttlMillis, contentSizeKB and peakRequestRateSeconds from Hub-V1 are no longer provided.

You can now use PUT to create and update channels idempotently.  The V1 POST and PATCH methods are still supported.

## monitoring

The Hub has monitoring available in:
* [New Relic](https://rpm.newrelic.com/accounts/565031/applications#filter=hub-v2)
* [Grafana](https://www.hostedgraphite.com/805cc7bf/b9f046ac-8ec4-4141-9e6d-5edc2b73e4d5/grafana/#/dashboard/db/hubv2)

Hub Servers:
* Staging - hub-v2-0<1-3>.cloud-east.staging
* Int - hub-v2-int-0<1-3>.cloud-east.staging
* Dev - hub-v2-0<1-3>.cloud-east.dev

## development

The Hub is a work in progress.  If you'd like to contribute, let us know.

The latest builds are in [Jenkins](http://ops-jenkins01.cloud-east.dev/view/hub-v2/)

[Install locally](https://github.com/flightstats/hubv2/wiki/Install-the-Hub-locally)

General Rules for Development:
* Only pull from master
* Create a new branch for features and bugs, avoiding '/' in the branch name
* Push a build from the branch to dev using [hub-v2-dev-cycle](http://ops-jenkins01.cloud-east.dev/view/hub-v2/job/hub-v2-dev-cycle/)
* after testing in dev, create a pull request from the feature branch to master
* every merge to master kicks off [hub-v2-int-cycle](http://ops-jenkins01.cloud-east.dev/view/hub-v2/job/hub-v2-int-cycle/) to hub-v2-int

## deployments

Any specific version of the Hub can be manually deployed to any environment using [hub-v2-deploy](http://ops-jenkins01.cloud-east.dev/view/hub-v2/job/hub-v2-deploy/)

Releases can also be manually kicked off from each machine in a cluster using the version number from Jenkins.
```
sudo salt-call triforce.deploy s3://triforce_builds/hubv2/hub-v2-<version>.tgz staging
```

## Requirements Notes

**No missing data**: As an API user I want to be able to query data and know that the order is fixed. If I query a “finished” bucket, then the quantity and order of that bucket will remain the same.

**Almost real time**:  As an API user I want to get the most recent data as soon as possible.  {Hub Devs: what is the SLA for ASAP?}

**Read out in the same order written**:  As an API user, for certain use cases, I want a guarantee that I can read the data out in the exact order I wrote it into the hub channel.

**Next n items**:  As an API user, I would like to get the next n items from the channel.
http://hub-v2/year/month/day/hour/minute/second/{keySuffix}/next/n
