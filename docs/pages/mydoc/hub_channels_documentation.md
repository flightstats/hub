---
title: Documentation
keywords: channel, documentation
last_updated: March 23, 2017
tags: [channel]
summary: 
sidebar: mydoc_sidebar
permalink: hub_channels_documentation.html
folder: hub
---

## Channel Documentation

This feature provides a place for a single [Markdown][1] document to be stored and presented at the ```/doc``` endpoint of a channel.

    http://hub/channel/stumptown/doc


# Reading the docs

By default the raw document will be returned for a ```GET``` request.

**Request:**

```bash
curl -X GET http://hub/channel/stumptown/doc
```

**Response:** ```200 OK```

```markdown
# Stumptown Channel
Great coffee provided over TCP/IP!
```

If the ```text/html``` [Accept header][2] is provided the document will be parsed as [Markdown][1] and rendered as HTML.

**Request:**

```bash
curl -X GET http://hub/channel/stumptown/doc -H 'Accept: text/html'
```

**Response:** ```200 OK```

```html
<h1>Stumptown Channel</h1>
<p>Great coffee provided over TCP/IP!</p>
```

# Updating the docs

Creating a new document requires a PUT request with your document sent to the ```/doc``` endpoint of your channel. 

**Request:**

```bash
curl -X PUT http://hub/channel/stumptown/doc -d 'Your documentation goes here!'
```

**Response:** ```200 OK```

```
Your documentation goes here!
```


# Deleting the docs

Deleting just requires a ```DELETE``` request be sent to the ```/doc``` endpoint of your channel.

**Request:**

```bash
curl -X DELETE http://hub/channel/stumptown/doc
```

**Response:** ```204 No Content```


[1]: https://daringfireball.net/projects/markdown/syntax
[2]: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept
