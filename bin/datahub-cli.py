#!/usr/bin/python

# FlightStats DataHub commandline client

import sys
import getopt
import readline
import re

def usage():
	print("Usage: datahub-cli.py --server <host[:port]>")

class DataHub(object):
	def __init__(self, server):
		self._server = server
		self._done = False
		self._channel = None
	def run(self):
		while(not self._done):
			line = raw_input('DataHub@%s> ' %(self._server))
			self.process_line(line)
	def help(self):
		print("Here are some common commands:")
		print("  channel <chan> : Set/show the current channel.")
		print("  help           : Show this screen")
		print("  quit           : Quit or exit")
	def process_line(self, line):
		if(line == "exit" or line == "quit"):
			print("Let's hub again real soon!")
			self._done = True
			return	
		elif(line == "help"):
			return self.help()
		elif(line.startswith("chan")):
			parts = re.split("\s+", line)
			if(len(parts) > 1):
				self._channel = parts[1]
			print("The current channel is '%s'" %(self._channel))
			return
		print("Command not understood -- try 'help'")

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
