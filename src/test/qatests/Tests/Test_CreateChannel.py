__author__ = 'gnewcomb'

import DH_helpers as dhh
import FStest_helpers as fstest
import unittest
import text_utils as txu
import unittest_helpers as uh
import requests
import httplib
import time
import thread
import json as jsonMod
import datetime

_mainDH = None

def getDataHub():
    global _mainDH
    if (not _mainDH):
        _mainDH = dhh.DataHub(debug=True)

    return _mainDH


#class NoParams(unittest.TestCase):
#
#    @classmethod
#    def setUpClass(cls):
#        cls.AllTripsResource = triph.AllTrips(debug=True)
#        cls.id, cls.createRes = cls.AllTripsResource.post(callParams = {}, isItinSource=True)
#
#        cls.TripResource = triph.Trip(id=cls.id, debug=True)
#        cls.getRes = cls.TripResource.get()
#        print('GET response: ', cls.getRes.json())
#
#    @attr('acceptance')
#    def test_no_params(self):
#    #        print('**********\r\n', NoParams.getRes.json())
#        self.assertIsNotNone(NoParams.id)
#        self.assertEqual(NoParams.createRes.status_code, httplib.CREATED)
#
#    @attr('acceptance')
#    def test_confirm_no_fields_on_GET(self):
#        self.body = NoParams.getRes.json()
#        for key in ['description', 'itineraryName', 'itineraryReferenceNumber', 'itineraryId', '_user',
#                    'name']:
#            self.assertIsNone(self.body[key])
#        self.assertEqual(0, len(self.body['flights']))
#        self.assertEqual(NoParams.TripResource.getUri(), str(self.body['_self']))
#        self.assertEqual(NoParams.id, str(self.body['id']))

class UnspecifiedTTL(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.DH = getDataHub()
        cls.channelName = txu.text_generator(min=10, max=20, includeSpace=False)
        cls.cn, cls.cnCreationResponse = cls.DH.post({'name': cls.channelName})

    def test_returns_201(self):
        self.assertEqual(UnspecifiedTTL.cnCreationResponse.status_code, httplib.CREATED)

    def test_uri_is_correct(self):
        cn = UnspecifiedTTL.cn
        meta, getRes = cn.get()
        self.assertEqual(meta.uri, cn.uri)

    def test_response_has_correct_structure(self):
        meta = dhh.ChannelMetadata(UnspecifiedTTL.cnCreationResponse.json())
        self.assertIsNotNone(meta)
        self.assertIsNotNone(meta.uri)
        self.assertIsNotNone(meta.latest)
        self.assertIsNotNone(meta.ws)
        self.assertIsNotNone(meta.name)
        self.assertIsNotNone(meta.creationDate)
        self.assertIsNotNone(meta.ttl)

    def test_location_header_is_correct(self):
        self.assertEqual(UnspecifiedTTL.cnCreationResponse.headers['location'], UnspecifiedTTL.cn.uri)

    def test_name_is_correct(self):
        self.assertEqual(UnspecifiedTTL.cnCreationResponse.json()['name'], UnspecifiedTTL.channelName)

    def test_creation_date_is_correct(self):
        myDt = fstest.parseFSDateTime(UnspecifiedTTL.cnCreationResponse.json()['creationDate'])
        print('myDt: ', myDt)
        print('now: ', datetime.datetime.now())
        self.assertTrue(fstest.areDatesClose(myDt, datetime.datetime.utcnow()))


#    it('TTL has numeric value', function() {

#    it('TTL defaults to 120 days', function() {
