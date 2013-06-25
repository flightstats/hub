#!/usr/bin/python

# FlightStats DataHub commandline client

import sys
import getopt
import re
import httplib
import mimetypes
import time
import readline # Leave this if you like history


def usage():
    print("Usage: datahub-cli.py --server <host[:port]>")


class DataHub(object):
    def __init__(self, server):
        self._server = server
        self._done = False
        self._channel = None
        self._prev = None
        self._next = None

    def run(self):
        while not self._done:
            try:
                line = raw_input('DataHub@%s> ' % self._server)
            except EOFError:
                print("\nSee ya!")
                break
            self.process_line(line)

    def help(self):
        print("Here are some common commands:")
        print("  mkchan <chan> [ttl] : Create a new channel")
        print("  channel <chan>      : Set/show the current channel")
        print("  meta                : Show current channel metadata")
        print("  post <text>         : Post text to the current channel")
        print("  postfile <file>     : Post text to the current channel")
        print("  get <id>            : Fetch item from channel by id")
        print("  getfile <id> <file> : Save id item into a file")
        print("  latest              : Fetch the latest item from the current channel")
        print("  latestfile <file>   : Save the latest item into a file")
        print("  previous            : Fetch the previous item")
        print("  next                : Fetch the next item")
        print("  list                : List all channel names")
        print("  ? or help           : Show this screen")
        print("  quit                : Quit or exit")

    def process_line(self, line):
        line = re.sub(r'^\s*', '', line)
        if line == "exit" or line == "quit":
            print("Let's hub again real soon!")
            self._done = True
            return
        elif line in ("?", "help"):
            return self.help()
        elif line == "list":
            return self._list_channels()
        elif line.startswith("chan"):
            parts = re.split("\s+", line)
            if len(parts) > 1:
                self._channel = parts[1]
                self._prev = None
                self._next = None
            print("The current channel is '%s'" % self._channel)
            return
        elif line.startswith("meta"):
            return self._show_metadata()
        elif line.startswith("getfile"):
            (cmd, identifier, filename) = re.split("\s*", line)
            return self._get_file(identifier, filename)
        elif line.startswith("get"):
            identifier = re.sub(r'^get\s*', '', line)
            self._do_get(identifier)
            return
        elif line.startswith("mkchan"):
            channel_name = re.sub(r'^mkchan\s*', '', line)
            ttl = None
            if (re.match(".*\s+\w+$", channel_name)):
                [channel_name, ttl] = re.split("\s*", channel_name)
            return self._create_channel(channel_name, ttl)
        elif line.startswith("latefile"):
            filename = re.sub(r'^latefile\s*', '', line)
            return self._get_latest(filename)
        elif line.startswith("late"):
            return self._get_latest()
        elif line.startswith("pr"):
            return self._get_previous()
        elif line.startswith("ne"):
            return self._get_next()
        elif line.startswith("postfile"):
            filename = re.sub(r'^postfile\s*', '', line)
            return self._postfile(filename)
        elif line.startswith("post"):
            return self._do_post(line)
        print("Command not understood -- try 'help'")

    def _do_post(self, line):
        line = re.sub(r'^post\s*', '', line)
        if len(line):
            content = line
        else:
            print("Go wild and use EOF to end it.")
            content = self._read_multiline()
        self._send_to_channel(content, 'text/plain')

    def _postfile(self, filename):
        try:
            f = open(filename, 'rb')
            contents = f.read()
            f.close()
            mime_type = mimetypes.guess_type(filename)[0]
            self._send_to_channel(contents, mime_type)
        except IOError as e:
            print("Unable to open/read file: %s", e)

    def _do_get(self, identifier):
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", "/channel/%s/%s" % (self._channel, identifier), None, dict())
        response = conn.getresponse()
        print(response.status, response.reason)
        self._show_response_if_text(response)

    def _get_file(self, identifier, filename):
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", "/channel/%s/%s" % (self._channel, identifier), None, dict())
        response = conn.getresponse()
        self._save_response_to_file(response, filename)

    def _save_response_to_file(self, response, filename):
        print(response.status, response.reason)
        f = open(filename, 'w')
        f.write(response.read())
        f.close()
        print("Saved %s bytes into file %s" % (self._find_header(response, 'content-length'), filename))

    def _get_previous(self):
        if not self._prev:
            print("No previous available.")
            return
        print("Fetching previous item: %s" % self._prev)
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", self._prev, None, dict())
        response = conn.getresponse()
        self._prev = self._extract_link(self._find_prev_link(response))
        self._next = self._extract_link(self._find_next_link(response))
        print(response.status, response.reason)
        self._show_response_if_text(response)

    def _list_channels(self):
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", "/channel", None, dict())
        response = conn.getresponse()
        print(response.status, response.reason)
        self._show_response_if_text(response)

    def _get_next(self):
        if not self._next:
            print("No next available.")
            return
        print("Fetching next item: %s" % self._next)
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", self._next, None, dict())
        response = conn.getresponse()
        self._prev = self._extract_link(self._find_prev_link(response))
        self._next = self._extract_link(self._find_next_link(response))
        print(response.status, response.reason)
        self._show_response_if_text(response)

    def _get_latest(self, filename=None):
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", "/channel/%s/latest" % self._channel, None, dict())
        response = conn.getresponse()
        print(response.status, response.reason)
        if response.status == 404:
            print("Not found (channel is empty or nonexistent)")
        elif response.status == 303:
            location = self._find_header(response, 'location')
            print("Fetching latest: %s" % location)
            conn = httplib.HTTPConnection(self._server)
            conn.request("GET", location, None, dict())
            response = conn.getresponse()
            self._prev = self._extract_link(self._find_prev_link(response))
            self._next = self._extract_link(self._find_next_link(response))
            if filename:
                self._save_response_to_file(response, filename)
            else:
                print(response.status, response.reason)
                self._show_response_if_text(response)

    def _show_response_if_text(self, response):
        content_type = self._find_header(response, 'content-type')
        if self._can_render_content_type(content_type):
            print(response.read())
        else:
            print("Non-text content type: %s" % content_type)
            print("Refusing to show content (try getfile)")

    def _find_next_link(self, response):
        link_headers = self._find_headers(response, "link")
        if link_headers:
            candidates = link_headers[0][1].split(', ')
            next_link = filter(lambda x: "next" in x, candidates)
            if len(next_link):
                return next_link[0]
        return None

    def _find_prev_link(self, response):
        link_headers = self._find_headers(response, "link")
        if link_headers:
            candidates = link_headers[0][1].split(', ')
            prev_link = filter(lambda x: "previous" in x, candidates)
            if len(prev_link):
                return prev_link[0]
        return None

    def _find_header(self, response, header_name):
        return self._find_headers(response, header_name)[0][1]

    def _find_headers(self, response, header_name):
        return filter(lambda x: x[0] == header_name, response.getheaders())

    def _extract_link(self, value):
        if value:
            return re.sub(r'.*<(.*)>.*', r'\1', value)

    def _can_render_content_type(self, content_type):
        return (content_type.startswith("text/") or
                content_type in ("application/json", "application/xml", "application/html", "application/javascript"))
        # others tbd

    def _send_to_channel(self, content, mime_type):
        conn = httplib.HTTPConnection(self._server)
        headers = {'Content-type': mime_type, 'Accept': 'application/json'}
        conn.request("POST", "/channel/%s" % self._channel, content, headers)
        response = conn.getresponse()
        print(response.status, response.reason)
        print(response.read())

    def _create_channel(self, channel_name, ttl=None):
        conn = httplib.HTTPConnection(self._server)
        if (ttl):
            if (ttl == 'null' or ttl == 'None'):
                content = '{"name": "%s", "ttl": null}' % (channel_name)
            else:
                content = '{"name": "%s", "ttl": "%s"}' % (channel_name, ttl)
        else:
            content = '{"name": "%s"}' % (channel_name)
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        start = time.time()
        conn.request("POST", "/channel", content, headers)
        response = conn.getresponse()
        self._channel = channel_name
        self._prev = None
        self._next = None
        print("Channel creation took %0.2f seconds" % (time.time() - start))
        print(response.status, response.reason)
        print(response.read())

    def _show_metadata(self):
        conn = httplib.HTTPConnection(self._server)
        conn.request("GET", "/channel/%s" % self._channel, None, dict())
        response = conn.getresponse()
        print(response.status, response.reason)
        print(response.read())

    def _read_multiline(self):
        lines = list()
        while True:
            try:
                line = raw_input('>')
                lines.append(line)
            except EOFError:
                break
        return '\n'.join(lines)


def main(argv):
    try:
        opts, args = getopt.getopt(argv, "s:", ["server="])
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    server = ''
    for opt, arg in opts:
        if opt == ('-?', '--help'):
            usage()
            sys.exit(2)
        elif opt in '-s' '--server':
            server = arg
    if not server:
        usage()
        sys.exit(2)
    print("Ok, we'll talk to the DataHub server at %s" % server)
    return DataHub(server).run()


if __name__ == "__main__":
    main(sys.argv[1:])
