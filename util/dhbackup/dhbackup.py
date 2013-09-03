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
            ws = websocket.WebSocketApp(ws_uri, on_message=self.on_message, on_error=self.on_error, on_close=self.on_close)
            ws.run_forever()
            print("NO LONGER RUNNING FOREVER.  Universe over.  Insert coin.")

    def on_message(self, ws, message):
        print("Queuing: %s" %(message))
        self._queue.put(message, block=True)

    def on_error(ws, error):
        print(error)

    def on_close(ws):
        print("### closed ###")

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
        while (True):
            try:
                uri = self._new_uri_queue.get(block=True)
                # print "READ from queue: %s" %(uri)
                headers = self._fetch_and_save_content(uri)
                previous_uri = self._find_previous(headers)
                self._download_complete_queue.put((uri, previous_uri), block=True)
                # print "Done fetching and saving for %s" %(uri)
            except Exception as e:
                print("Failed to fetch content: %s" %(e))

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
        if (self._path_format is not None):
            subdirectory = strftime(self._path_format, creation_date.timetuple())
        full_path = "%s/%s/%s.gz" % (self._directory, subdirectory, file_name)
        return re.sub("//+", "/", full_path)

    def _find_previous(self, headers):
        for link in headers['link'].split(','):
            if(re.match(".*rel=\"previous\"", link.strip())):
                return re.sub(r".*<(http.*)>;.*", r"\1", link)
        return None

class CatchupWorker:
    def __init__(self, new_uri_queue, download_complete_queue):
        self._new_uri_queue = new_uri_queue
        self._download_complete_queue = download_complete_queue
        self._earliest = None
        self._history = dict()
    def run(self):
        while True:
            uri, previous_uri = self._download_complete_queue.get(block=True)
            print "Catchup worker sees: %s -> %s" %(uri, previous_uri)
            self._history[uri] = previous_uri
            if self._earliest is None:
                self._earliest = uri
            elif previous_uri not in self._history:
                print "GAP DETECTED"
                self._new_uri_queue.put(previous_uri, block=True)
                continue

            candidates = []
            current = uri
            while current is not None and current != self._earliest:
                previous = self._history[current]
                candidates.append(previous)
                if previous == self._earliest:
                    print "Removing: %s" %(candidates)
                    [self._history.pop(x) for x in candidates]
                    print "size is now: %d" %(len(self._history))
                    self._earliest = uri
                    break
                current = previous

"""
if previous not in history,
    push previous to queue
else

"""


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument("channel_uri", help="The URI of the channel to back up")
    parser.add_argument("-d", "--dir", help="The directory to save data into", required=True)
    parser.add_argument("-p", "--path", help="The date format to use when creating subdirectories (see python's strftime)", required=False)
    args = parser.parse_args(argv)

    print("Backing up data from channel " + args.channel_uri + " to " + args.dir)

    new_uri_queue = Queue()
    download_complete_queue = Queue()

    websocket_listener = ChannelWebsocketListener(args.channel_uri, new_uri_queue)
    content_writer = ContentWriter(new_uri_queue, download_complete_queue, args.dir, args.path)
    catchup_worker = CatchupWorker(new_uri_queue, download_complete_queue)

    content_writer_process = Process(target=content_writer.run)
    catchup_process = Process(target=catchup_worker.run)

    content_writer_process.start()
    catchup_process.start()
    websocket_listener.listen()

    content_writer_process.join()
    catchup_process.join()


if __name__ == "__main__":
    main(sys.argv[1:])
