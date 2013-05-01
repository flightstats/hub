#!/usr/bin/python
#
# This is a small utility to test single-threaded, single-channel
# datahub write performance.
#
# It requires the httplib2 module

from __future__ import print_function
import sys
import httplib2
import getopt
from time import time

class RedSpeedo(object):
	def __init__(self, channel_url, count, size, quiet):
		self._channel_url = channel_url
		self._count = count
		self._size = size
		self._quiet = quiet
	def swim(self):
		http = httplib2.Http(".cache")
		headers = {'content-type': 'text/plain'}
		content = 'X' * self._size
		start_time = time()
		for i in range(self._count):
			r, c = http.request(self._channel_url, 'POST', headers=headers, body=content)
			if(self._quiet):
			    sys.stdout.write('.')
			    sys.stdout.flush()
			else:
			    print(r)
		end_time = time()
		if(self._quiet):
			print('')
		delta = end_time - start_time
		writes_per_sec = self._count / delta
		time_per_write = delta / self._count
		print("%.4f writes / second" % (writes_per_sec))
		print("%.4f seconds / write" % (time_per_write))

def usage():
	print("Usage: red_speedo <options>\n")
	print("where <options> are:\n")
	print("  --chan <channel uri>  :: Specify the URI of the channel to post to (must exist)")
	print("  --count <num>         :: Number of times to write to the channel")
	print("  --size <bytes>        :: Size of each write")
	print("  --quiet               :: Don't print each response")


def main(argv):
	try:
		opts, args = getopt.getopt(argv, "c:n:s:", ["chan=","count=","size=","quiet"])
	except getopt.GetoptError:
		usage()
		sys.exit(2)
	chan, count, size, quiet = None, None, None, False
	for opt,arg in opts:
		if(opt == ('-?', '--help')):
			usage()
			sys.exit(2)
		elif(opt in ('-c' '--chan')):
			chan = arg
		elif(opt in ('-n', '--count')):
			count = arg
		elif(opt in ('-s', '--size')):
			size = arg
		elif(opt in ('-q', '--quiet')):
			quiet = True
	if(chan is None or count is None or size is None):
		usage()
		sys.exit(2)
	print("Ok, let's swim against the 'hub at %s..." %(chan))
	return RedSpeedo(chan, int(count), int(size), quiet).swim()

if __name__ == "__main__":
	main(sys.argv[1:])
