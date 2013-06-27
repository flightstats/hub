#!/usr/bin/python
#
# This is a small utility to test single-threaded, single-channel
# datahub read performance.
#
# It requires the httplib2 module

from __future__ import print_function
import sys
import re
import getopt
import json
from time import time

import httplib2


class Peregrine(object):
    def __init__(self, channel_url, num, quiet):
        self._channel_url = channel_url
        if (num is None):
            self._num = None
        else:
            self._num = int(num)
        self._quiet = quiet

    def stoop(self):
        current_uri = self._find_latest()
        start_time = time()

        http = httplib2.Http(".cache")
        method = 'GET'
        if (self._quiet):
            method = 'HEAD'

        ct = 0
        while (current_uri is not None):
            ct = ct + 1
            print(current_uri)
            r, c = http.request(current_uri, method)
            link = r['link']
            current_uri = self._parse_previous(link)
            if (self._num is not None and (ct >= self._num)):
                break

        end_time = time()
        delta = end_time - start_time
        read_per_sec = ct / delta
        time_per_read = delta / ct
        print("%.4f reads / second" % read_per_sec)
        print("%.4f seconds / read" % time_per_read)

    def _find_latest(self):
        http = httplib2.Http(".cache")
        print("Fetching channel metadata...")
        r, c = http.request(self._channel_url, 'GET')
        meta = json.loads(c)
        latest_uri = meta['_links']['latest']['href']
        r, c = http.request(latest_uri, 'HEAD')
        location = r['content-location']
        return location

    def _parse_previous(self, link):
        links = link.split(', ')
        url = None
        for link in links:
            if (re.match(r'<(.*)>;rel="previous"', link)):
                url = re.sub(r'<(.*)>;rel="previous"', r'\1', link)
        return url


def usage():
    print("Usage: peregrine <options>\n")
    print("where <options> are:\n")
    print("  --chan <channel uri>  :: Specify the URI of the channel to read from")
    print("  --num <num>           :: Max number of items to fetch (default = all)")
    print("  --quiet               :: Use HEAD instead of GETting full payloads")


def main(argv):
    try:
        opts, args = getopt.getopt(argv, "c:n:", ["chan=", "num=", "quiet"])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    chan, num, quiet = None, None, False
    for opt, arg in opts:
        if opt == ('-?', '--help'):
            usage()
            sys.exit(2)
        elif opt in ('-c' '--chan'):
            chan = arg
        elif opt in ('-n', '--num'):
            num = arg
        elif opt in ('-q', '--quiet'):
            quiet = True
    if chan is None:
        usage()
        sys.exit(2)
    print("Ok, let's swoop the stoop: %s..." % (chan))
    return Peregrine(chan, num, quiet).stoop()


if __name__ == "__main__":
    main(sys.argv[1:])
