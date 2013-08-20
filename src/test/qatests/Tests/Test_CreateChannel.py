__author__ = 'gnewcomb'

from nose.plugins.attrib import attr
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
import sys

_mainDH = None

def getDataHub():
    global _mainDH
    if (not _mainDH):
        _mainDH = dhh.DataHub(debug=True)

    return _mainDH

def confirmChannelBody(channelMetadata):
    """
    Takes a ChannelMetadata instance and returns True if expected properties are present, else False.
    """
    required_properties = ('uri', 'latest', 'ws', 'name', 'creationDate', 'ttlMillis')
    for p in required_properties:
        if (not hasattr(channelMetadata, p)):
            return False
    return True

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
        meta = UnspecifiedTTL.cn.metadata
        self.assertTrue(confirmChannelBody(meta))

    def test_location_header_is_correct(self):
        self.assertEqual(UnspecifiedTTL.cnCreationResponse.headers['location'], UnspecifiedTTL.cn.uri)

    def test_name_is_correct(self):
        self.assertEqual(UnspecifiedTTL.cn.metadata.name, UnspecifiedTTL.channelName)

    def test_creation_date_is_correct(self):
        myDt = fstest.parseFSDateTime(UnspecifiedTTL.cn.metadata.creationDate)
        print('myDt: ', myDt)
        print('now: ', datetime.datetime.now())
        self.assertTrue(fstest.areDatesClose(myDt, datetime.datetime.utcnow()))

#    @attr('only')
    def test_TTL_is_numeric(self):
        self.assertTrue(str.isdigit(str(UnspecifiedTTL.cn.metadata.ttlMillis)))

    def test_TTL_defaults_to_120_days(self):
        self.assertEqual(UnspecifiedTTL.cn.metadata.ttlMillis, 10368000000)

class SpecifiedTTL(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.DH = getDataHub()
        cls.channelName = txu.text_generator(min=10, max=20, includeSpace=False)
        cls.channelTTLMillis = 25000
        cls.cn, cls.cnCreationResponse = cls.DH.post({'name': cls.channelName, 'ttlMillis': cls.channelTTLMillis})

    def test_returns_201(self):
        self.assertEqual(SpecifiedTTL.cnCreationResponse.status_code, httplib.CREATED)

    def test_response_has_correct_structure(self):
        meta = SpecifiedTTL.cn.metadata
        self.assertTrue(confirmChannelBody(meta))

    def test_has_correct_TTL_value(self):
        self.assertEqual(SpecifiedTTL.cn.metadata.ttlMillis, SpecifiedTTL.channelTTLMillis)
