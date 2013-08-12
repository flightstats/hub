#!/usr/bin/python
#
# This will use a websocket to listen to a channel and will then fetch each item and save it to a file.
#
import os
import sys
import argparse
import json
import gzip

import websocket
import httplib2


class Backuper:
    def __init__(self, directory, channel):
        self._channel_url = channel
        self._directory = directory
        self._http = None

    def listen(self):
        if not os.path.exists(self._directory):
            os.makedirs(self._directory)
        self._http = httplib2.Http(".cache")
        meta = self._load_metadata()
        ws_uri = meta['_links']['ws']['href']
        print ws_uri
        while True:
            ws = websocket.WebSocketApp(ws_uri, on_message=self.on_message)
            ws.run_forever()

    def on_message(self, ws, message):
        r, c = self._http.request(message, 'GET')
        fileName = message.split("/").pop()
        fullPath = self._directory + "/" + fileName + ".gz"
        output = gzip.open(fullPath, "wb")
        output.write(c)
        output.close()

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self._channel_url, 'GET')
        return json.loads(c)


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument("chan", help="The URI of the channel to back up")
    parser.add_argument("-d", "--dir", help="The directory to save data into", required=True)
    args = parser.parse_args(argv)

    print("Saving data from channel " + args.chan + " to " + args.dir)
    # print("Listening to %s..." % (chan))
    return Backuper(args.dir, args.chan).listen()


if __name__ == "__main__":
    main(sys.argv[1:])
