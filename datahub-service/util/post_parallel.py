#!/usr/bin/python
#
# This is a small utility to post to a channel in parallel
#
# It requires the httplib2 module

from __future__ import print_function
import sys
import re
import getopt
import json
from time import time
import threading
import httplib2

def usage():
    print("Usage: post_parallel <options>\n")
    print("where <options> are:\n")
    print("  --chan <channel uri>  :: Specify the URI of the channel to post to")
    print("  --num <num>           :: Number of writes to do concurrently")

class PostingThread(threading.Thread):
    def __init__(self, semaphore, event, num, channel_url):
        threading.Thread.__init__(self)
        self._semaphore = semaphore
        self._event = event
        self._num = num
        self._channel_url = channel_url
    def run(self):
        http = httplib2.Http(".cache")
        headers = {'content-type': 'text/plain'}
        content = "Thread %d has posted to you.  You're welcome" %(self._num)

        print("Thread %d is releasing semaphore..." %(self._num))
        self._semaphore.release()   # increment internal counter
        self._event.wait()
        print("Thread %d is posting to channel..." %(self._num))
        r, c = http.request(self._channel_url, 'POST', headers=headers, body=content)


def main(argv):
    try:
        opts, args = getopt.getopt(argv, "c:n:", ["chan=", "num="])
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
    if chan is None:
        usage()
        sys.exit(2)
    print("Ok, let's post in parallel...")

    semaphore = threading.Semaphore()
    event = threading.Event()

    for i in range(0, int(num)):
        thread = PostingThread(semaphore, event, i, chan)
        thread.start()

    for i in range(0, int(num)):
        semaphore.acquire()

    event.set()

if __name__ == "__main__":
    main(sys.argv[1:])
