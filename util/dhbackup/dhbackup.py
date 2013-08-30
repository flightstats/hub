#!/usr/bin/python
#
# This will use a websocket to listen to a channel and will then fetch each item and save it to a file.
#
import os
import sys
import argparse
import json
import gzip
import re
import websocket
import httplib2
import dateutil.parser
from urlparse import urlsplit
from time import strftime
from multiprocessing import Process, Queue


class ChannelWebsocketListener:
    def __init__(self, channel, queue):
        self._channel_url = channel
        self._queue = queue
        self._http = None

    def listen(self):
        self._http = httplib2.Http(".cache")
        ws_uri = self._find_websocket_uri()
        print ws_uri
        while True:
            ws = websocket.WebSocketApp(ws_uri, on_message=self.on_message)
            ws.run_forever()

    def on_message(self, ws, message):
        print "Queuing: %s" %(message)
        self._queue.put(message, block=True)

    def _find_websocket_uri(self):
        meta = self._load_metadata()
        return meta['_links']['ws']['href']

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self._channel_url, 'GET')
        return json.loads(c)


class ContentWriter:
    def __init__(self, queue, directory, path_format):
        self._queue = queue
        self._directory = directory
        self._path_format = path_format
        self._http = httplib2.Http(".cache")

    def run(self):
        while (True):
            uri = self._queue.get(block=True)
            print "READ from queue: %s" %(uri)
            self._fetch_and_save_content(uri)

    def _fetch_and_save_content(self, uri):
        r, c = self._http.request(uri, 'GET')
        creation_date = dateutil.parser.parse(r['creation-date'])
        filename = self._build_filename(uri, creation_date)
        directory = os.path.dirname(filename)
        self._make_dir_if_missing(directory)
        output = gzip.open(filename, "wb")
        output.write(c)
        output.close()

    def _make_dir_if_missing(self, directory):
        if not os.path.exists(directory):
            print("Creating directory: %s" % directory)
            os.makedirs(directory)

    def _build_filename(self, message, creation_date):
        url_path = urlsplit(message).path
        file_name = url_path.split("/")[-1]
        subdirectory = ""
        if (self._path_format is not None):
            subdirectory = strftime(self._path_format, creation_date.timetuple())
        full_path = "%s/%s/%s.gz" % (self._directory, subdirectory, file_name)
        return re.sub("//+", "/", full_path)


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument("channel_uri", help="The URI of the channel to back up")
    parser.add_argument("-d", "--dir", help="The directory to save data into", required=True)
    parser.add_argument("-p", "--path", help="The date format to use when creating subdirectories (see python's strftime)", required=False)
    args = parser.parse_args(argv)

    print("Saving data from channel " + args.channel_uri + " to " + args.dir)
    queue = Queue()
    content_writer = ContentWriter(queue, args.dir, args.path)
    process = Process(target=content_writer.run)
    process.start()
    ChannelWebsocketListener(args.channel_uri, queue).listen()
    process.join()


if __name__ == "__main__":
    main(sys.argv[1:])
