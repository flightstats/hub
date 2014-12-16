The Hub V2
=======

* [overview](#overview)
* [consistency](#consistency)
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
* [fetch latest channel item](#fetch-latest-channel-item)
* [tag interface](#tag-interface)
* [time interface](#time-interface)
* [subscribe to events](#subscribe-to-events)
* [group callback interface](#group-callback-interface)
* [provider interface](#provider-interface)
* [delete a channel](#delete-a-channel)
* [configure replication](#configure-replication)
* [replication status](#replication-status)
* [stop replication](#stop-replication)
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
* In development: http://hub-v2.svc.dev/
* In staging: http://hub-v2.svc.staging/ (coming soon!)
* In production: http://hub-v2.svc.prod/ (coming soon!)

## overview

The Hub is designed to be a fault tolerant, highly available service for data storage and distribution.  Most features are available via a REST API.

It currently only supports time series channels.  Note: The sequence channel API is deprecated and will be supported separately as [Hub V1](https://github.com/flightstats/hub).

Channels represent uniquely addressable items that are iterable and query-able by time.  Each item may be up to to 10 MB. 

The [encrypted-hub](#encrypted-hub) (EH) is a separate installation of the Hub.
The features and API of the EH are mostly the same as the Hub, with a few additions.

## consistency

* All times from the Hub are in UTC.
* By default all iteration, queries, group callbacks and websockets return stable data.  Data is considered stable when iteration will provide consistent results.
* All requests for a specific item by id will return that item if it exists.  If the item is within the unstable window, it will not include iterator links.

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

* Why does /latest return an empty array of uris?

  Most likely the last data added to that channel is older than the time to live (ttlDays).

* How can I guarantee ordering in a channel?

  You can wait for the response for an item before writing the next item.  

## hub resources

To explore the Resources available in the Hub, go to http://hub-v2/

## list channels

To obtain the list of channels:

`GET http://hub/channel`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://hub/channel/stumptown"
    }, {
      "name" : "ptown",
      "href" : "http://hub/channel/ptown"
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

**V2 Note**:
Channel items type, ttlMillis, contentSizeKB and peakRequestRateSeconds from V1 are no longer provided.
While PUT is shown here, the V1 POST to http://hub/channel/ is still supported.

`PUT http://hub/channel/stumptown`

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
            "href": "http://hub/channel/stumptown"
        },
        "latest": {
            "href": "http://hub/channel/stumptown/latest"
        },
        "ws": {
            "href": "ws://hub/channel/stumptown/ws"
        },
        "time": {
            "href": "http://hub/channel/stumptown/time"
        },
        "dayQuery": {
             "href": "http://hub/channel/stumptown/{year}/{month}/{day}"
        },
        "hourQuery": {
             "href": "http://hub/channel/stumptown/{year}/{month}/{day}/{hour}"
        },
        "minuteQuery": {
             "href": "http://hub/channel/stumptown/{year}/{month}/{day}/{hour}/{minute}"
        },
        "secondQuery": {
             "href": "http://hub/channel/stumptown/{year}/{month}/{day}/{hour}/{minute}/{second}"
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlDays": 14,
    "description": "a sequence of all the coffee orders from stumptown",
    "tags": ["coffee"]
}
```

Here's how you can do this with curl:
```bash
curl -i -X PUT --header "Content-type: application/json"  http://hub/channel/stumptown
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format
(currently, only `ttlDays`, `description` and `tags` can be updated).
Each of these fields is optional.

**V2 Note**:
While PUT is shown here, the V1 PATCH to http://hub/channel/channelname is still supported.

`PUT http://hub/channel/channelname`


## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://hub/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://hub/channel/stumptown`

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` header is optional:

```
POST http://hub/channel/stumptown
Content-type: text/plain
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://hub/channel/stumptown"
    },
    "self" : {
      "href" : "http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}"
    }
  },
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:

```bash
curl -i -X POST --header "Content-type: text/plain" --data 'your content here' http://hub/channel/stumptown
```

## fetch content from channel

To fetch content that was stored into a hub channel, do a `GET` on the `self` link in the above response:

`GET http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}/previous>;rel="previous"
Link: <http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}/next>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
There are two `Link` headers that provide links to the previous and next items in the channel.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

## fetch latest channel items

To retrieve the latest item inserted into a channel, issue a HEAD or GET request on the `latest` link 
returned from the channel metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/2013/04/23/20/42/31/749/{hash}`

Here is how you can do this with curl:

`curl -I http://hub/channel/stumptown/latest`

## tag interface

To retrieve all of the tags in the Hub:

`GET http://hub/tag`

On success: `HTTP/1.1 200 OK`
```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/tag"
    },
    "tags" : [ {
      "name" : "orders",
      "href" : "http://hub/tag/orders"
    }, {
      "name" : "coffee",
      "href" : "http://hub/tag/coffee"
    } ]
  }
}
```

Any of the returned tag links can be followed to see all of the channels with that tag:

`GET http://hub/tag/coffee`

On success: `HTTP/1.1 200 OK`
```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/tag/coffee"
    },
    "channels" : [ {
      "name" : "stumptown",
      "href" : "http://hub/channel/stumptown"
    }, {
      "name" : "spella",
      "href" : "http://hub/channel/spella"
    } ]
  }
}
```

## latest time interface

TODO 

The time interface returns all of the URIs of items inserted within the specified minute.

To see time format options, issue a GET request on the `time` link returned from the channel metadata.

`GET http://hub/channel/stumptown/time`

```json
{
    "_links": {
        "self": {
            "href": "http://localhost:9080/channel/test_0_8147969620767981/time"
        },
        "second": {
            "href": "http://localhost:9080/channel/test_0_8147969620767981/time/second"
        },
        "minute": {
            "href": "http://localhost:9080/channel/test_0_8147969620767981/time/minute"
        },
        "hour": {
            "href": "http://localhost:9080/channel/test_0_8147969620767981/time/hour"
        },
        "day": {
            "href": "http://localhost:9080/channel/test_0_8147969620767981/time/day"
        }
    }
}
```

Call one of uris, and the Hub will issue a 303 redirect for the current time with the specified resolution.

`HEAD http://localhost:9080/channel/test_0_8147969620767981/time/second`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/2014/01/13/10/42/31

A GET on the returned URI will return all of the content URIs within that period.

`GET http://hub/channel/stumptown/2014/01/13/10/42/31`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel/stumptown/2014/01/13/10/42/31"
    },
    "uris" : [ 
        "http://hub/channel/stumptown/2014/01/13/10/42/31/149/{hash1}", 
        "http://hub/channel/stumptown/2014/01/13/10/42/31/359/{hash2}",
        "http://hub/channel/stumptown/2014/01/13/10/42/31/642/{hash3}" 
    ]
  }
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items inserted within the minute so far, and will
change as other items are inserted.

### time resolution

You can request all of the items by the time resolution you specify in the URL.  
For all the items in a minute: `GET http://hub/channel/stumptown/2014/01/13/10/42`
For all the items in an hour: `GET http://hub/channel/stumptown/2014/01/13/10`

You can also access the urls via convenience methods:

`HEAD http://hub/channel/stumptown/time/minute`
`HEAD http://hub/channel/stumptown/time/hour`

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
http://hub/channel/stumptown/2014/01/13/10/42/31/149/{hash1}
http://hub/channel/stumptown/2014/01/13/10/42/31/359/{hash2}
http://hub/channel/stumptown/2014/01/13/10/42/31/642/{hash3}
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

To see all existing group callbacks and status:

`GET http://hub/group`
 
To create a new group callback:

`PUT http://hub/group/{name}`

``` json
{
  "callbackUrl" : "http://client/path/callback",
  "channelUrl" : "http://hub/channel/stumptown",
  "parallelCalls" : 2
}
```

Once a Group is created, it can not be changed, only deleted.  Put may be safely called multiple times with the same 
 configuration.

To see the current configuration of a group callback:

`GET http://hub/group/{name}`

To delete a group callback:

`DELETE http://hub/group/{name}`

Delete will return a 202, and it may take up to a minute to properly stop a group from servicing the callback.

#### Behavior

The group listening to the `callbackUrl` will get a payload POSTed to it for every new item in the channel, starting at the time the group is created.  
200 is considered a successful response.  Any other response is considered an error, and will cause the server to retry.   Redirects are allowed.                                        
Retries will use an exponential backoff up to one minute, and the server will continue to retry at one minute intervals indefinitely.

``` json
{
  "name" : "stumptownCallback",
  "uris" : [ "http://hub/channel/stumptown/2014/01/13/10/42/31/759/{hash1}" ]
}
```

If a client is running in Sungard, latency may limit throughput to ~3 per second.

## provider interface

For external data providers, there is a simplified interface suitable for exposing to the authenticated outside world.

`POST http://hub/provider/`

* it creates a channel if it doesn't exist
* it expects a `channelName` header
* does not support any other HTTP methods
* does not return any links
* access by external data providers is controlled through a proxy maintained by Operations

## delete a channel

**Channel Deletion will be access controlled in Staging and Prod environments**

To delete a channel when after you no longer need it, simply issue a `DELETE` command to that channel.
Delete returns a 202, indicating that the request has been accepted, and will take an indeterminate time to process.

 `DELETE http://hub/channel/stumptown`

Here's how you can do this with curl:
```bash
curl -i -X DELETE http://hub/channel/stumptown
```

## configure replication

**Replication Configuration will be access controlled in in Staging and Prod environments**

The Hub can replicate Sequence channels from another Hub instance. 

The Source Hub has the channel(s) you want replicated.
The Target Hub is where you want those channel(s).
Replication is configured on the Target Hub, and does not have any requirements on the Source Hub, other than it must
implement the standard Hub API for Sequence channels.

For example, if you want to replicate the channel `pdx` from Source: http://hub.svc.prod/ to Target: http://hub.svc.staging/

GET the existing configuration for replication:

`GET http://hub.svc.staging/replication/hub.svc.prod`

* Content-type: application/json

```json
{
   "historicalDays" : 10,
   "excludeExcept" : [ "stumptown" ]
}
```

Modify the existing replication configuration to include `pdx`:

`PUT http://hub/replication/hub.other`

* Content-type: application/json

```json
{
   "historicalDays" : 10,
   "excludeExcept" : [ "stumptown", "pdx" ]
}
```

To remove a single channel from Replication, use PUT without that channel.  To remove `stumptown`:

`PUT http://hub/replication/hub.other`

* Content-type: application/json

```json
{
   "historicalDays" : 10,
   "excludeExcept" : [ "pdx" ]
}
```

To delete Replication for an entire domain, use DELETE:

`DELETE http://hub/replication/hub.other`

## v1 to v2 replication

When replicating from hub v1 to hub v2, the location urls will change from v1 sequences to v2 time formats.
The creation time of the original item will stay the same.

## Replication Details

* Modifications to existing replication configuration take effect immediately.
* If you are replicating a channel into HubB from HubA, and you will be prevented from inserting data into that channel on HubB.
* `channels` means "Replicate listed channels".
* `historicalDays` tells the replicator how far back in time to start. Zero (the default) means "only get new values".
* If http://hub.other/channel/stumptown `ttlDays` is 10, and `historicalDays` is 5, only items from the last 5 days will be replicated.
* If a channel's `historicalDays` is 0 and the ongoing replication is restarted, replication will continue with the existing sequence if it is up to `historicalDays` + 1 old.

As an example, the replicating cluster is going into a maintenance window, and a domain has `historicalDays` of zero.  The entire cluster is stopped at noon.
Maintenance takes longer than expected and the cluster resumes at 11 AM the next day.  Replication will pick up where it left off, and will eventually catch up to the current position.
If, instead, the cluster didn't start until 1 PM the next day, one hour's worth will not be replicated.

You can see the configuration for a single domain at:

 `GET http://hub/replication/hub.other`

  ```json
  {
    "domain" : "hub.other",
    "historicalDays" : 10,
    "channels" : [ "stumptown", "pdx" ]
  }
  ```

## replication status

TODO figure out replicationLatestFormat

You can get the status of all current replication domains at:

 `GET http://hub/replication`

 ```json
 {
   "domains" : [ {
     "domain" : "datahub.svc.staging",
     "historicalDays" : 0,
     "channels" : [ "positionsSynthetic" ]
   }, {
     "domain" : "hub.svc.prod",
     "historicalDays" : 10,
     "channels" : [ "provider_icelandair" ]
   } ],
   "status" : [ {
     "replicationLatest" : '',
     "sourceLatest" : 6187114,
     "connected" : true,
     "deltaLatest" : 0,
     "name" : "positionsSynthetic",
     "url" : "http://datahub.svc.staging/channel/positionsSynthetic/"
   }, {
     "replicationLatest" : 3717,
     "sourceLatest" : 3717,
     "connected" : false,
     "deltaLatest" : 0,
     "name" : "provider_icelandair",
     "url" : "http://hub.svc.prod/channel/provider_icelandair/"
   }
   ]
 }
 ```

## stop replication

**Stopping Replication will be access controlled in in Staging and Prod environments**

Stop replication of the entire domain `hub.other`, issue a `DELETE` command.

`DELETE http://hub/replication/hub.other`

On success: `HTTP/1.1 202 Accepted`. This is Accepted because the replication may be on another server, which should
be notified within seconds.  The user should verify that replication is stopped.


## health check

The Health Check returns a 200 status code when the server can connect to each data store.
If the server can not access a data store, it will return a 500 status code.

Responding to http connections is the last step on startup, so the health check will be unresponsive until startup is complete.
On shutdown, the server immediately stops responding to new http connections, so there are no separate codes for startup and shutdown.

`GET http://hub/health`

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

`GET http://hub/channel/stumptown_encrypted_audit/1007`

```json
{
  "user": "somebody",
  "uri": "http://hub/channel/stumptown_encrypted/1005",
  "date": "2014-05-22T20:56:08.739Z"
}
```

The EH is available at:

* In development: http://encrypted-hub-v2.svc.dev/ (soon!)
* In staging: http://encrypted-hub-v2.svc.staging/ (soon!)
* In production: http://encrypted-hub-v2.svc.prod/ (soon!)

## api changes from v1 to v2

Channel items type, ttlMillis, contentSizeKB and peakRequestRateSeconds from Hub-V1 are no longer provided.


## monitoring

The Hub has monitoring available in:
* [New Relic](https://rpm.newrelic.com/accounts/565031/applications#filter=hub)
* [Graphite Prod](http://svcsmon.cloud-east.prod/dashboard/#hub)
* [Graphite Staging](http://svcsmon.cloud-east.staging/dashboard/#hub)
* [Graphite Dev](http://svcsmon.cloud-east.dev/dashboard/#hub)

Hub Servers:
* Prod - hub-0<1-3>.cloud-east.prod
* Staging - hub-0<1-3>.cloud-east.staging
* Dev - hub-0<1-3>.cloud-east.dev

Encrypted Hub Servers:
* Dev - encrypted-hub-0<1-3>.cloud-east.dev

## development

The Hub is a work in progress.  If you'd like to contribute, let us know.

The latest builds are in [Jenkins](http://ops-jenkins01.cloud-east.dev/view/hub/)

To run Java based tests and jasmine-node tests locally, you will most likely want to use DynamoDB Local.
Install it from http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html
and then start it running with `java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -inMemory`

Once DynamoDBLocal is running, create src/main/resources/default_local.properties and add the following value:

```
dynamo.endpoint=localhost:8000
```

To run the jasmine-node based integration tests:
```
jasmine-node .
```

You can also run all of the integration tests from gradle with:
```
gradle integrationTests
```

The Hub uses the Client Team's [Develop-Master branching strategy](http://wiki.office/wiki/Client_Team_Operational_Documentation#Git_Usage_Diagram).
Rules:
* Only pull from master
* merge feature branches to develop, which [builds and deploys](http://ops-jenkins01.cloud-east.dev/job/hub-develop/) to dev with version `DEVELOP.mm-dd.#`
* after testing in dev, create a pull request from the feature branch to master
* every merge to master kicks off [build and deploy](http://ops-jenkins01.cloud-east.dev/job/hub-staging/) to staging with version ``
* develop is reset to master every day at 6 AM [hub-develop-daily-reset](http://ops-jenkins01.cloud-east.dev/job/hub-develop-daily-reset/)

## deployments

The Hub is deployed to [Dev](http://hub.svc.dev/health) after each successful build in [Jenkins](http://ops-jenkins01.cloud-east.dev/job/hub/)

Deployments to Staging can be manually run from [Hub Tasks](http://ops-jenkins01.cloud-east.dev/job/hub/batchTasks/)

Releases to Prod currently must be manually kicked off from each machine using the version number from Jenkins.
```
sudo salt-call triforce.deploy s3://triforce_builds/hubv2/hub-v2-<version>.tgz prod
```

## Requirements Notes

**No missing data**: As an API user I want to be able to query data and know that the order is fixed. If I query a “finished” bucket, then the quantity and order of that bucket will remain the same.

**Almost real time**:  As an API user I want to get the most recent data as soon as possible.  {Hub Devs: what is the SLA for ASAP?}

**Read out in the same order written**:  As an API user, for certain use cases, I want a guarantee that I can read the data out in the exact order I wrote it into the hub channel.

**Next n items**:  As an API user, I would like to get the next n items from the channel.
http://hub/year/month/day/hour/minute/second/{keySuffix}/next/n
