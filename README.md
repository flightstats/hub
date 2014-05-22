The Hub
=======

* [overview](#overview)
* [clients](#clients)
* [error handling](#error-handling)
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
* [provider interface](#provider-interface)
* [delete a channel](#delete-a-channel)
* [configure replication](#configure-replication)
* [replication status](#replication-status)
* [stop replication](#stop-replication)
* [health check](#health-check)
* [access control](#access-control)
* [encrypted-hub](#encrypted-hub)
* [api updates](#api-updates)
* [monitoring](#monitoring)
* [development](#development)
* [deployments](#deployments)

For the purposes of this document, the Hub is at http://hub/.

* On your local machine it is at: http://localhost:9080/
* In development: http://hub.svc.dev/
* In staging: http://hub.svc.staging/
* In production: http://hub.svc.prod/

## overview

The Hub is designed to be a fault tolerant, highly available service for data distribution.  Most features are available via a REST API.

It currently only supports Sequence channels.

Sequence channels represent a linked list of data.  Each item added gets a sequential id.  They are traversable, and can support items up to 10 MB.
Sequence channels are intended for insertation rates less than five items per second.
Users should note that with high frequency inserts, the clients view of insertion order may not be maintained.

The [encrypted-hub](#encrypted-hub) (EH) is a separate installation of the Hub.
The features and API of the EH are mostly the same as the Hub, with a few additions.

## clients

Since the Hub provides an http interface, most tools which support http can access the Hub.
Examples on this page are in curl, most GETs will work in a browser.

In addition, some more sophisticated clients exist:
* [Java](https://github.com/flightstats/datahub-client/)
* [Javascript](https://github.com/flightstats/edge-lib/tree/master/datahub)
* [Utilities](https://github.com/flightstats/hub-utilities)

## error handling

Clients should consider handling transient server errors (500 level return codes) with retry logic.  This helps to ensure that transient issues (networking, etc)
  do not prevent the client from entering data. For Java clients, this framework provides many options - https://github.com/rholder/guava-retrying
The Hub team recommends clients use exponential backoff.

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

`type` is optional, and defaults to Sequence.  Only `Sequence` is a valid value.

`ttlDays` is optional and should be a positive number. If not specified, a default value of 120 days is used.
`ttlMillis` is still accepted as an input parameter, and is converted to ttlDays.  A `null` ttlMillis is converted to the default 120 days.

`peakRequestRateSeconds` and `contentSizeKB` are optional.

`description` is optional and defaults to an empty string.  This text field can be up to 1024 bytes long.

`tags` is an optional array of string values.  Tag values are limited to 48 characters, and may only contain `a-z`, `A-Z` and `0-9`.
A channel may have at most 20 tags.

`POST http://hub/channel`

* Content-type: application/json

```json
{
   "name": "stumptown",
   "type": "Sequence",
   "ttlDays": "14",
   "peakRequestRateSeconds":1,
   "contentSizeKB":10,
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
        }
    },
    "name": "stumptown",
    "creationDate": "2013-04-23T20:25:33.434Z",
    "ttlDays": 14,
    "type": "Sequence",
    "contentSizeKB" : 10,
    "peakRequestRateSeconds" : 10,
    "description": "a sequence of all the coffee orders from stumptown",
    "tags": ["coffee"]
}
```

Here's how you can do this with curl:
```bash
curl -i -X POST --header "Content-type: application/json" --data '{"name": "stumptown"}' http://hub/channel
```

## update a channel

Some channel metadata can be updated. The update format looks much like the channel create format
(currently, only `ttlDays`, `description`, `contentSizeKB`, `peakRequestRateSeconds` and `tags` can be updated).
Each of these fields is optional.
Attempting to change other fields will result in a 400 error.

`PATCH http://hub/channel/channelname`

* Content-type: application/json

```json
{
   "ttlDays": 21,
   "contentSizeKB" : 20,
   "peakRequestRateSeconds" : 5,
   "description": "the sequence of all coffee orders from stumptown pdx",
   "tags": ["coffee", "orders"]
}
```

On success:  `HTTP/1.1 200 OK`, and the new channel metadata is returned (see example return data from create channel).

Here's how you can do this with curl:
```bash
curl -i -X PATCH --header "Content-type: application/json" \
    --data '{"ttlDays": 21, "contentSizeKB" : 20, "peakRequestRateSeconds" : 5, \
    "description": "the sequence of all coffee orders from stumptown pdx", "tags": ["coffee", "orders"]}' \
    http://hub/channel/stumptown
```

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://hub/channel/stumptown`

On success: `HTTP/1.1 200 OK`  (see example return data from create channel).

Here's how you can do this with curl:

`curl http://hub/channel/stumptown`

## insert content into channel

To insert data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported).  The `Content-Encoding` and
`Content-Language` headers are optional:

```
POST http://hub/channel/stumptown
Content-type: text/plain
Content-Language: en
Content-Encoding: gzip
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 201 Created`

`Location: http://hub/channel/stumptown/1000`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://hub/channel/stumptown"
    },
    "self" : {
      "href" : "http://hub/channel/stumptown/1000"
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

`GET http://hub/channel/stumptown/1001`

On success: `HTTP/1.1 200 OK`
```
Content-Type: text/plain
Creation-Date: 2013-04-23T00:21:30.662Z
Link: <http://hub/channel/stumptown/1000>;rel="previous"
Link: <http://hub/channel/stumptown/1002>;rel="next"
...other.headers...

your content here
```

Note: The `Content-Type` will match the Content-Type used when inserting the data.  
For Sequence channels, there are two `Link` headers that provide links to the previous and next items in the channel.
If you are fetching the latest item, there will be no next link.
The `Creation-Date` header will correspond to when the data was inserted into the channel.

Here's how you can do this with curl:

`curl -i http://hub/channel/stumptown/1000`

## fetch latest channel item

To retrieve the latest item inserted into a Sequence channel, issue a HEAD or GET request on the `latest` link returned from the channel
metadata.  The Hub will issue a 303 redirect.

`HEAD http://hub/channel/stumptown/latest`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/1010`

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

## time interface

The time interface returns all of the URIs of items inserted within the specified minute.

To see the time format, issue a GET request on the `time` link returned from the channel metadata.
The Hub will issue a 303 redirect for the current time.

`HEAD http://hub/channel/stumptown/time`

On success:  `HTTP/1.1 303 See Other`
`Location: http://hub/channel/stumptown/time/2014-01-13T10:42+0000`

A GET on the returned URI will return all of the content URIs within that period.
The time format is the ISO 8601 extended format with minute resolution.  In Java it is "yyyy-MM-dd'T'HH:mmZ" and in Javascript using Moment.js it is "YYYY-MM-DDTHH:mmZ"

`GET http://hub/channel/stumptown/time/2014-01-13T12:42-0800`

On success:  `HTTP/1.1 200 OK`
Content-Type is `application/json`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://hub/channel/stumptown/time/2014-01-13T12:42-0800"
    },
    "uris" : [ "http://hub/channel/stumptown/1002", "http://hub/channel/stumptown/1003",
    "http://hub/channel/stumptown/1004", "http://hub/channel/stumptown/1005",
    "http://hub/channel/stumptown/1006", "http://hub/channel/stumptown/1007" ]
  }
```

If no items were submitted during that time, 'uris' is an empty array.
If the time requested is the current minute, 'uri's will reflect all of the items inserted within the minute so far, and will
change as other items are inserted.

## subscribe to events

While a common approach to consuming data from the hub involves traversing next/previous links, clients may
"subscribe" to single channel events by listening on a channel's websocket.  
In the channel metadata there is a `ws` link, and a websocket aware client may connect to this URL.

Clients should be aware that websockets are a "best effort" service, and are not stateful.  If the ws connection is lost,
which will happen on hub server restarts, the client will need to reconnect, and may have missed items.

Once connected, the line-oriented protocol is simple:

For Sequence channels, each time data is inserted into the channel, the hub will send a line to the client with the
URL for that content.

```
http://hub/channel/stumptown/1000
http://hub/channel/stumptown/1001
http://hub/channel/stumptown/1002
...etc...
```


## provider interface

For external data providers, there is a simplified interface suitable for exposing to the authenticated outside world.

`POST http://hub/provider/`

* it creates a Sequence channel if it doesn't exist
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

**Support for Replication is currently in Alpha.**

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

**Replication Details**

* Modifications to existing replication configuration take effect immediately.
* If you are replicating a channel into HubB from HubA, and you will be prevnted from inserting data into that channel on HubB.
* `excludeExcept` means "Exclude all of the channels, Except the ones specified".
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
    "excludeExcept" : [ "stumptown", "pdx" ]
  }
  ```

## replication status

You can get the status of all current replication domains at:

 `GET http://hub/replication`

 ```json
 {
   "domains" : [ {
     "domain" : "datahub.svc.staging",
     "historicalDays" : 0,
     "excludeExcept" : [ "positionsSynthetic" ]
   }, {
     "domain" : "hub.svc.prod",
     "historicalDays" : 10,
     "excludeExcept" : [ "provider_icelandair" ]
   } ],
   "status" : [ {
     "replicationLatest" : 6187114,
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

* In development: http://encrypted-hub.svc.dev/
* In staging: http://encrypted-hub.svc.staging/ (soon!)
* In production: http://encrypted-hub.svc.prod/ (soon!)

## api updates

Also, there are a couple of small API changes that clients should be aware of.

`ttlMillis` is deprecated, users should use `ttlDays` instead.  Existing `ttlMillis` values are rounded up to the nearest day.

`lastUpdated` is no longer provided in the channel metadata.  If you need to know the latest value, you can use the `/latest` interface

## monitoring

The Hub has monitoring available in:
* [New Relic](https://rpm.newrelic.com/accounts/565031/applications#filter=hub)
* [Graphite Prod](http://svcsmon.cloud-east.prod/dashboard/#hub)
* [Graphite Staging](http://svcsmon.cloud-east.staging/dashboard/#hub)
* [Graphite Dev](http://svcsmon.cloud-east.dev/dashboard/#hub)

## development

The Hub is a work in progress.  If you'd like to contribute, let us know.

The latest builds are in [Jenkins](http://ops-jenkins01.cloud-east.dev/job/hub/)

To run Java based tests and jasmine-node tests locally, you will most likely want to use DynamoDB Local.
Install it from http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Tools.DynamoDBLocal.html
and then start it running with `java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar`

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

## deployments

The Hub is deployed to [Dev](http://hub.svc.dev/health) after each successful build in [Jenkins](http://ops-jenkins01.cloud-east.dev/job/hub/)

Deployments to Staging can be manually run from [Hub Tasks](http://ops-jenkins01.cloud-east.dev/job/hub/batchTasks/)

Releases to Prod currently must be manually kicked off from each machine using the version number from Jenkins.
```
sudo salt-call triforce.deploy s3://triforce_builds/hub/hub-<version>.tgz prod
```