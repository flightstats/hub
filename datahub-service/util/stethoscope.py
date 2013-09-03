#!/usr/bin/python
#
# This will use a websocket to listen to a channel and will then fetch each item and display it on the console.
#
import sys
import getopt
import json
import websocket
import httplib2

class Stethoscope:
    def __init__(self, channel):
        self._channel_url = channel
        self._http = None

    def listen(self):
        self._http = httplib2.Http(".cache")
        meta = self._load_metadata()
        ws_uri = meta['_links']['ws']['href']
        print ws_uri
        ws = websocket.WebSocketApp(ws_uri,on_message = self.on_message)
        ws.run_forever()

    def on_message(self, ws, message):
        r, c = self._http.request(message, 'GET')
        print("-----------------------------------------------------------------------------------------------------\n%s" %(c))

    def _load_metadata(self):
        print("Fetching channel metadata...")
        r, c = self._http.request(self._channel_url, 'GET')
        return json.loads(c)

def usage():
    print("Usage: stethoscope <options>\n")
    print("where <options> are:\n")
    print("  --chan <channel uri>  :: Specify the URI of the channel to listen to")


def main(argv):
    try:
        opts, args = getopt.getopt(argv, "c:", ["chan="])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    chan, count, size, quiet = None, None, None, False
    for opt, arg in opts:
        if opt == ('-?', '--help'):
            usage()
            sys.exit(2)
        elif opt in ('-c' '--chan'):
            chan = arg
    if chan is None:
        usage()
        sys.exit(2)
    print("Listening to %s..." % (chan))
    return Stethoscope(chan).listen()


if __name__ == "__main__":
    main(sys.argv[1:])
