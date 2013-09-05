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
from urlparse import urlsplit
from time import strftime
from multiprocessing import Process, Queue

import websocket
import httplib2
import dateutil.parser


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
        self._queue.put(message, block=True)

    def _find_websocket_uri(self):
        meta = self._load_metadata()
        return meta['_links']['ws']['href']

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self._channel_url, 'GET')
        return json.loads(c)


class ContentWriter:
    def __init__(self, new_uri_queue, download_complete_queue, directory, path_format):
        self._new_uri_queue = new_uri_queue
        self._download_complete_queue = download_complete_queue
        self._directory = directory
        self._path_format = path_format
        self._http = httplib2.Http(".cache")

    def run(self):
        while True:
            uri = None
            try:
                uri = self._new_uri_queue.get(block=True)
                headers = self._fetch_and_save_content(uri)
                previous_uri = self._find_previous(headers)
                self._download_complete_queue.put((uri, previous_uri), block=True)
            except Exception as e:
                print("Failed to fetch content at %s: %s" % (uri, e))

    def _fetch_and_save_content(self, uri):
        r, c = self._http.request(uri, 'GET')
        creation_date = dateutil.parser.parse(r['creation-date'])
        filename = self._build_filename(uri, creation_date)
        directory = os.path.dirname(filename)
        self._make_dir_if_missing(directory)
        output = gzip.open(filename, "wb")
        output.write(c)
        output.close()
        return r

    def _make_dir_if_missing(self, directory):
        if not os.path.exists(directory):
            print("Creating directory: %s" % directory)
            os.makedirs(directory)

    def _build_filename(self, message, creation_date):
        url_path = urlsplit(message).path
        file_name = url_path.split("/")[-1]
        subdirectory = ""
        if self._path_format is not None:
            subdirectory = strftime(self._path_format, creation_date.timetuple())
        full_path = "%s/%s/%s.gz" % (self._directory, subdirectory, file_name)
        return re.sub("//+", "/", full_path)

    def _find_previous(self, headers):
        for link in headers['link'].split(','):
            if re.match(".*rel=\"previous\"", link.strip()):
                return re.sub(r".*<(http.*)>;.*", r"\1", link)
        return None


class CatchupWorker:
    def __init__(self, new_uri_queue, download_complete_queue, memento):
        self._new_uri_queue = new_uri_queue
        self._download_complete_queue = download_complete_queue
        self._memento = memento
        self._earliest = None
        self._history = dict()

    def run(self):
        uri = self._memento.load_state()
        if uri:
            self._earliest = uri
            self._history[uri] = ""
        while True:
            uri, previous_uri = self._download_complete_queue.get(block=True)
            self._handle_new_uri(uri, previous_uri)

    def _handle_new_uri(self, uri, previous_uri):
        self._history[uri] = previous_uri
        if self._earliest is None:
            self._update_earliest_and_persist(uri)
        elif previous_uri not in self._history:
            print("GAP DETECTED: Requesting backfill for %s" % previous_uri)
            self._new_uri_queue.put(previous_uri, block=True)
            return
        candidates = self._find_completed_chain(uri)
        if len(candidates):
            [self._history.pop(x) for x in candidates]
            if len(candidates) > 1:
                print("Catchup worker purged %d catchup records" % (len(candidates)))
            self._update_earliest_and_persist(uri)

    def _find_completed_chain(self, uri):
        results = []
        while uri is not None and uri in self._history and uri != self._earliest:
            previous = self._history[uri]
            results.append(previous)
            if previous == self._earliest:
                return results
            uri = previous
        return []

    def _update_earliest_and_persist(self, uri):
        self._earliest = uri
        previous = self._history[uri]
        self._memento.update(uri, previous)


class ProgressMemento:
    def __init__(self, directory, channel_name):
        self._directory = directory
        self._channel_name = channel_name

    def update(self, new_uri, previous):
        f = open(self._build_filename(), "w")
        f.write("%s" % new_uri)
        f.close()

    def load_state(self):
        filename = self._build_filename()
        if not os.path.exists(filename):
            return None
        f = open(filename, "r")
        result = f.read()
        f.close()
        return result

    def _build_filename(self):
        return "%s/%s.progress.txt" % (self._directory, self._channel_name)


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument("channel_uri", help="The URI of the channel to back up")
    parser.add_argument("-d", "--dir", help="The directory to save data into", required=True)
    parser.add_argument("-p", "--path", help="The date format to use when creating subdirectories (see python's strftime)", required=False)
    args = parser.parse_args(argv)

    print("Backing up data from channel " + args.channel_uri + " to " + args.dir)

    new_uri_queue = Queue()
    download_complete_queue = Queue()

    memento = ProgressMemento(args.dir, channel_name_from_channel_uri(args.channel_uri))

    websocket_listener = ChannelWebsocketListener(args.channel_uri, new_uri_queue)
    content_writer = ContentWriter(new_uri_queue, download_complete_queue, args.dir, args.path)
    catchup_worker = CatchupWorker(new_uri_queue, download_complete_queue, memento)

    content_writer_process = Process(target=content_writer.run)
    catchup_process = Process(target=catchup_worker.run)

    content_writer_process.start()
    catchup_process.start()
    websocket_listener.listen()

    content_writer_process.join()
    catchup_process.join()


def channel_name_from_channel_uri(uri):
    return urlsplit(uri).path.split("/")[-1]


if __name__ == "__main__":
    main(sys.argv[1:])