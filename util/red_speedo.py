#!/usr/bin/python
#
# This is a small utility to test single-threaded, single-channel
# datahub write performance.
#
# It requires the httplib2 module

import httplib2

channel_url = 'http://datahub-01.cloud-east.dev:8080/channel/lolcats'

h = httplib2.Http(".cache")
for i in range(1000):
	r, c = h.request(channel_url, 'POST', headers={'content-type': 'text/plain'}, body='a man a plan a canal panama')
	print r
