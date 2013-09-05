from src.test.qatests.PythonTests import DH_helpers as dhh

__author__ = 'gnewcomb'

from nose.plugins.attrib import attr
import FStest_helpers as fstest
import unittest
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
        _mainDH = dhh.DataHub(debug=False)

    return _mainDH

def confirmChannelBody(channelMetadata, required_props=('uri', 'latest', 'ws', 'name', 'creationDate', 'ttlMillis')):
    """
    Takes a ChannelMetadata instance and returns True if expected properties are present, else False.
    """
    for p in required_props:
        if (not hasattr(channelMetadata, p)):
            return False
    return True

class UnspecifiedTTL(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.DH = getDataHub()
        cls.channelName = dhh.Channel.makeRandomChannelName()
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
        cls.channelName = dhh.Channel.makeRandomChannelName()
        cls.channelTTLMillis = 25000
        cls.cn, cls.cnCreationResponse = cls.DH.post({'name': cls.channelName, 'ttlMillis': cls.channelTTLMillis})

    def test_returns_201(self):
        self.assertEqual(SpecifiedTTL.cnCreationResponse.status_code, httplib.CREATED)

    def test_response_has_correct_structure(self):
        meta = SpecifiedTTL.cn.metadata
        self.assertTrue(confirmChannelBody(meta))

    def test_has_correct_TTL_value(self):
        self.assertEqual(SpecifiedTTL.cn.metadata.ttlMillis, SpecifiedTTL.channelTTLMillis)

class NameTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.DH = getDataHub()

    def test_name_may_contain_underscores(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.name = randomName[:10] +'_'+ randomName[10:]
        self.channel, self.response = NameTests.DH.post({'name': self.name})
        self.assertEqual(self.response.status_code, httplib.CREATED)
        self.assertEqual(self.channel.metadata.name, self.name)

    @unittest.skip('Code not implemented')
    def test_name_may_not_match_Cassandra_reserved_word(self):
        self.channel, self.response = NameTests.DH.post({'name': 'channelMetadata'})
        self.assertTrue(fstest.isHTTPServerError(self.response.status_code))

    # See:  https://www.pivotaltracker.com/story/show/49566971
    def test_name_may_not_be_empty_string(self):
        self.channel, self.response = NameTests.DH.post({'name': ''})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

    def test_name_may_not_consist_only_of_whitespace(self):
        self.channel, self.response = NameTests.DH.post({'name': '    '})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

    @unittest.skip('BUG: https://www.pivotaltracker.com/story/show/51434073 - whitespace is trimmed from name')
    def test_whitespace_trimmed_from_name(self):
        self.name = ' '+ dhh.Channel.makeRandomChannelName() +' '
        self.channel, self.response = NameTests.DH.post({'name': self.name})
        self.assertEqual(self.response.status_code, httplib.CREATED)
        self.assertEqual(self.channel.metadata.name, self.name.strip())

#   see: https://www.pivotaltracker.com/story/show/51434189
    def test_name_may_not_contain_forward_slash(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.name = randomName[:10] +'/'+ randomName[10:]
        self.channel, self.response = NameTests.DH.post({'name': self.name})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

    def test_name_may_not_contain_space(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.name = randomName[:10] +' '+ randomName[10:]
        self.channel, self.response = NameTests.DH.post({'name': self.name})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

    def test_name_may_not_contain_dash(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.name = randomName[:10] +'-'+ randomName[10:]
        self.channel, self.response = NameTests.DH.post({'name': self.name})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

    def test_name_may_not_contain_high_ASCII_chars(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.name = randomName[:10] + unichr(300) + randomName[10:]
        self.channel, self.response = NameTests.DH.post({'name': self.name})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

# All of these are skipped currently
class TTLTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.DH = getDataHub()

    # see BUG: https://www.pivotaltracker.com/story/show/52425747
    def test_TTL_may_be_null_results_in_no_ttlMillis_property_returned(self):
        self.channel, self.response = TTLTests.DH.post({'name': dhh.Channel.makeRandomChannelName(), 'ttlMillis': None}, debug=True)
        self.assertEqual(self.response.status_code, httplib.CREATED)
        self.assertFalse(hasattr(self.channel.metadata, 'ttlMillis'))

    @unittest.skip('BUG: https://www.pivotaltracker.com/story/show/52486795')
    def test_TTL_may_not_be_negative(self):
        pass

    @unittest.skip('BUG: https://www.pivotaltracker.com/story/show/52486795')
    def test_TTL_may_not_be_zero(self):
        pass

    # TODO: blank or empty --> null, or disallowed?

    @unittest.skip('BUG: https://www.pivotaltracker.com/story/show/52486795')
    def test_TTL_may_not_contain_letters(self):
        pass

    @unittest.skip('BUG: https://www.pivotaltracker.com/story/show/52486795')
    def test_TTL_may_not_contain_a_period(self):
        pass

class OtherScenarios(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.DH = getDataHub()

    def test_empty_payload_not_allowed(self):
        self.channel, self.response = OtherScenarios.DH.post({})
        self.assertEqual(self.response.status_code, httplib.BAD_REQUEST)

    @unittest.skip('BUG: https://www.pivotaltracker.com/story/show/52507013 - parallel attempts to create channel with same name only allow one to be created')
    def test_parallel_attempts_to_create_channel_with_same_name_show_only_one_successful_response(self):
        # TODO: implement this
        pass

    def test_cannot_use_existing_channel_name(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.cn1, self.res1 = OtherScenarios.DH.post({'name': randomName})
        self.assertEqual(self.res1.status_code, httplib.CREATED)
        self.cn2, self.res2 = OtherScenarios.DH.post({'name': randomName})
        self.assertEqual(self.res2.status_code, httplib.CONFLICT)

    def test_can_create_two_channels_whose_name_differs_only_in_case(self):
        randomName = dhh.Channel.makeRandomChannelName()
        self.cn1, self.res1 = OtherScenarios.DH.post({'name': randomName +'x'})
        self.cn2, self.res2 = OtherScenarios.DH.post({'name': randomName +'X'})
        self.assertEqual(self.res1.status_code, httplib.CREATED)
        self.assertEqual(self.res2.status_code, httplib.CREATED)