#!/usr/bin/python

# FlightStats DataHub commandline client

import sys
import getopt
import readline
import re
import httplib, urllib

def usage():
	print("Usage: datahub-cli.py --server <host[:port]>")

class DataHub(object):
	def __init__(self, server):
		self._server = server
		self._done = False
		self._channel = None
	def run(self):
		while(not self._done):
			try:
				line = raw_input('DataHub@%s> ' %(self._server))
			except EOFError:
				print("\nSee ya!")
				break
			self.process_line(line)
	def help(self):
		print("Here are some common commands:")
		print("  mkchan <chan>  : Create a new channel")
		print("  channel <chan> : Set/show the current channel")
		print("  post <text>    : Post text to the channel")
		print("  get <id>       : Fetch item from channel by id")
		print("  latest         : Fetch the latest item from the current channel")
		print("  ? or help      : Show this screen")
		print("  quit           : Quit or exit")
	def process_line(self, line):
		if(line == "exit" or line == "quit"):
			print("Let's hub again real soon!")
			self._done = True
			return	
		elif(line in ("?", "help")):
			return self.help()
		elif(line.startswith("chan")):
			parts = re.split("\s+", line)
			if(len(parts) > 1):
				self._channel = parts[1]
			print("The current channel is '%s'" %(self._channel))
			return
		elif(line.startswith("get")):
			id = re.sub(r'^get\s*', '', line)
			self._do_get(id)
			return
		elif(line.startswith("post")):
			return self._do_post(line)
		elif(line.startswith("mkchan")):
			channel_name = re.sub(r'^mkchan\s*', '', line)
			return self._create_channel(channel_name)
		elif(line.startswith("late")):
			return self._get_latest()
		print("Command not understood -- try 'help'")
	def _do_post(self, line):
		line = re.sub(r'^post\s*', '', line)
		if(len(line)):
			content = line
		else:
			print("Go wild and use EOF to end it.")
			content = self._read_multiline()
		self._send_to_channel(content)
	def _do_get(self, id):
		conn = httplib.HTTPConnection(self._server)
		#print("DEBUG: /channel/%s/%s" %(self._channel, id))
		conn.request("GET", "/channel/%s/%s" %(self._channel, id), None, dict())
		response = conn.getresponse()
		print(response.status, response.reason)
		print(response.read())
	def _get_latest(self):
		conn = httplib.HTTPConnection(self._server)
		conn.request("GET", "/channel/%s/latest" %(self._channel), None, dict())
		response = conn.getresponse()
		print(response.status, response.reason)
		if(response.status == 404):
			print("Not found (channel is empty or nonexistent)")
		elif(response.status == 303):
			location = self._find_header(response, 'location')
			print("Fetching latest: %s" %(location))
			conn = httplib.HTTPConnection(self._server)
			conn.request("GET", location, None, dict())
			response = conn.getresponse()
			print(response.status, response.reason)
			print(response.read())
	def _find_header(self, response, header_name):
		return filter(lambda x: x[0] == header_name, response.getheaders())[0][1]
	def _send_to_channel(self, content):
		conn = httplib.HTTPConnection(self._server)
		headers = {'Content-type': 'text/plain', 'Accept': 'application/json'}
		conn.request("POST", "/channel/%s" %(self._channel), content, headers)
		response = conn.getresponse()
		print(response.status, response.reason)
		print(response.read())
	def _create_channel(self, channel_name):
		conn = httplib.HTTPConnection(self._server)
		content = '{"name": "%s"}' %(channel_name);
		headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
		conn.request("POST", "/channel", content, headers)
		response = conn.getresponse()
		self._channel = channel_name
		print(response.status, response.reason)
		print(response.read())
	def _read_multiline(self):
		lines = list()
		while(True):
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
	for opt,arg in opts:
		if(opt == ('-?', '--help')):
			usage()
			sys.exit(2)
		elif(opt in ('-s' '--server')):
			server = arg
	if(not server):
		usage()
		sys.exit(2)
	print("Ok, we'll talk to the DataHub server at %s" %(server))
	return DataHub(server).run()

if __name__ == "__main__":
	main(sys.argv[1:])
