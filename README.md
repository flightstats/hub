datahub
=======

The FlightStats Data Hub

The Data Hub is a robust platform for data distribution.  

There are some descriptions of this here:
http://confluence.office/display/TECH/Data+Hub

* [create a channel](#create-a-channel)
* [fetch channel metadata](#fetch-channel-metadata)
* [insert content into channel](#insert-content-into-channel)
* [fetch content from channel](#fetch-content-from-channel)
* [fetch latest channel item](#fetch-latest-channel-item)
* [subscribe to events](#subscribe-to-events)

For the purposes of this document, the datahub is at http://datahub:8080.

In development, it is actually at: http://datahub-01.cloud-east.dev:8080

## create a channel

`POST http://datahub:8080/channel`

* Content-type: application/json

```json
{  
   "name": "stumptown"
}
```

On success:  `HTTP/200 OK`

```json
{
    "_links": {
        "self": {
            "href": "http://datahub:8080/channel/stumptown"
        },
        "latest": {
            "href": "http://datahub:8080/channel/stumptown/latest"
        },
        "ws": {
            "href": "ws://datahub:8080/channel/stumptown/ws"
        }
    },
    "name": "your-channel-name",
    "creationDate": "2013-04-23T20:25:33.434Z"
}
```

Here's how you can do this with curl:
```bash
curl -i -X POST --header "Content-type: application/json" \
    --data '{"name": "stumptown"}'  \
    http://datahub:8080/channel
```

## fetch channel metadata

To fetch metadata about a channel, do a GET on its `self` link:

`GET http://datahub:8080/channel/stumptown`

On success: `HTTP/200 OK`

```json
{
  "_links" : {
    "self" : {
      "href" : "http://localhost:8080/channel/stumptown"
    },
    "latest" : {
      "href" : "http://localhost:8080/channel/stumptown/latest"
    },
    "ws" : {
      "href" : "ws://localhost:8080/channel/stumptown/ws"
    }
  },
  "lastUpdateDate" : "2013-04-23T20:36:35.310Z",
  "name" : "stumptown",
  "creationDate" : "2013-04-23T20:36:17.885Z"
}
```

## insert content into channel

## fetch content from channel

## fetch latest channel item

## subscribe to events
## Websockets:
