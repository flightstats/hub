# Tools for troubleshooting or testing

Before running any of these tools you will need to install their dependencies.

    $ npm install


## Timeout Server
This is an HTTP server that accepts requests, doesn't respond, and holds the socket open until the timeout is reached.

    $ node timeout_server.js --timeout 120000 --port 54321


## Callback Server
This is an HTTP server that logs incoming messages until closed (e.g. CTRL-C).

    $ node callback_server.js --port 54321


## Provider
Submits a request to the supplied URL every N milliseconds.

    $ node provider.js --url http://localhost:80/ --frequency 1000


## WebSocket Client
This is a WebSocket client that logs incoming messages until closed (e.g. CTRL-C).

    $ node websocket_client.js --url ws://localhost:80/

