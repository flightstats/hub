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

On success:  `HTTP/1.1 200 OK`

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

On success: `HTTP/1.1 200 OK`

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

Here's how you can do this with curl:

`curl http://datahub:8080/channel/stumptown`

## insert content into channel

To post data to a channel, issue a POST on the channel's `self` URI and specify the appropriate
content-type header (all content types should be supported):

```
POST http://datahub:8080/channel/stumptown
Content-type: text/plain
Accept: application/json
___body_contains_arbitrary_content
```

On success: `HTTP/1.1 200 OK`
`Location: http://datahub:8080/channel/stumptown/00002FHOK8JMK000`

```json
{
  "_links" : {
    "channel" : {
      "href" : "http://datahub:8080/channel/stumptown"
    },
    "self" : {
      "href" : "http://datahub:8080/channel/stumptown/00002FHOK8JMK000"
    }
  },
  "id" : "00002FHOK8JMK000",
  "timestamp" : "2013-04-23T20:42:31.146Z"
}
```

Here's how you could do this with curl:



## fetch content from channel

To fetch content that was stored into a datahub channel, do a `GET` on the `self` link in the above response:

`GET http://datahub:8080/channel/stumptown/00002FHOK8JMK000`

On success: `HTTP/1.1 200 OK`
`Content-type: whatever-you/put-in`
`payload body is what you put in`

## fetch latest channel item

## subscribe to events
## Websockets:
