__author__ = 'gnewcomb'

import re
import sys
import string
import FStest_helpers as fs
import text_utils as txu
import requests
import json as jsonMod
from urlparse import urlparse

DH_DOMAIN = 'datahub.svc.dev'

class ChannelMetadata():
    def __init__(self, JSON):
        try:
            self.uri = JSON['_links']['self']['href']
            self.latest = JSON['_links']['latest']['href']
            self.ws = JSON['_links']['ws']['href']
            self.name = JSON['name']
            self.creationDate = JSON['creationDate']
            self.ttlMillis = JSON['ttlMillis']
        except:
            msg = 'Error parsing channel metadata: '+ str(sys.exc_info()[0])
            raise ValueError(msg)

        # special case for lastUpdateDate, which is only on channel fetch and not channel create, so we won't bother
        #   requiring it
        try:
            self.lastUpdateDate = JSON['lastUpdateDate']
        except:
            self.lastUpdateDate = None

class ItemMetadata():
    def __init__(self, JSON, debug=False):
        if (debug):
            print('JSON dump:')
            print(jsonMod.dumps(JSON))
        try:
            self.channelUri = JSON['_links']['channel']['href']
            self.uri = JSON['_links']['self']['href']
            self.timestamp = JSON['timestamp']
        except:
            msg = 'Error parsing item metadata: '+ str(sys.exc_info()[0])
            raise ValueError(msg)

class DataHub(fs.FsResource):
    def __init__(self, domain=DH_DOMAIN, debug=False):
        fs.FsResource.__init__(self, domain=domain, debug=debug, uri=self.__getUri(domain))

    def __getUri(self, domain):
        return '/'.join(('http:/', domain, 'channel'))

    def patch(self, *args):
        raise NotImplementedError

    def put(self, *args):
        raise NotImplementedError

    def head(self, *args):
        raise NotImplementedError

    def delete(self, *args):
        raise NotImplementedError

    def post(self, callParams, headers={'content-type': 'application/json'}):
        """
        Create a channel.
        Returns Channel obj, response
        """
        self.headers = headers
        self.response = fs.FsResource.post(self, callParams, self.headers)
        myChannel = None
        if (fs.isHTTPSuccess(self.response.status_code)):
            try:
                myChannel = Channel(self.response.json(), self.debug)
            except ValueError as ex:
                print(ex.message)
        else:
            print('Did not receive success code trying to create new channel ', self.response.status_code)

        return myChannel, self.response

    def get(self, callParams={}):
        """
        Returns list of channels, response
        """
        self.response = fs.FsResource.get(self, callParams)
        channels = None
        if (fs.isHTTPSuccess(self.response.status_code)):
            try:
                channels = self.response.json()['_links']['channels']
            except KeyError as ex:
                print('Unable to parse GetAllChannels() JSON.')
        else:
            print('Did not receive success code trying to get all channels: ', self.response.status_code)
        return channels, self.response


class Channel(fs.FsResource):
    """
    To be created as the result of a POST to DataHub.
    Implements GET, POST, PATCH
    """
    def __init__(self, cnCreateJSON, debug=False):
        self.metadata = ChannelMetadata(cnCreateJSON)
        parsed = urlparse(self.metadata.uri)
        fs.FsResource.__init__(self, domain=parsed.netloc, debug=debug, uri=self.metadata.uri)

    def get(self, callParams={}):
        """
        Returns instance of ChannelMetadata, response.
        """
        self.response = fs.FsResource.get(self, callParams)
        if (fs.isHTTPSuccess(self.response.status_code)):
            try:
                self.metadata = ChannelMetadata(self.response.json())
            except ValueError as ex:
                print(ex.message)
        else:
            print('Did not receive success code fetching channel metadata ', self.response.status_code)

        return self.metadata, self.response

    def post(self, data, headers={'content-type': 'text/plain'}):
        """
        Inserts data into a channel. To stream the data, use the 'with' statement as shown here:
            http://docs.python-requests.org/en/latest/user/advanced/#streaming-uploads
        Returns instance of Item, response.
        """
        self.headers = headers
        self.payload = data
        if (self.debug):
            self.report()
        print ('Calling POST at {}', self.uri)
        self.response = None

        try:
            self.response = requests.post(self.uri, data=self.payload, headers=self.headers)
        except:
            print('Error in POST call at {}: {}'.format(self.uri, sys.exc_info()[0]))

        if (not fs.isHTTPSuccess(self.response.status_code)):
            print('Did not get success response from item POST: ', self.response.response_code)
            return None, self.response

        itemMeta = myItem = None
        try:
            itemMeta = ItemMetadata(self.response.json())
            myItem = Item(itemMeta.uri)
        except ValueError as ex:
            print(ex.message)
        return myItem, self.response

    def getLatestUri(self):
        """
        Calls HEAD on the 'latest' uri from a GET on a channel.
        Returns uri, HEAD response.
        """
        cnMeta, getRes = self.get({})

        if (None == cnMeta):
            return None, getRes

        latestUri = response = None
        try:
            response = requests.head(cnMeta.latest)
            latestUri = response.headers['location']
        except:
            print('Failed to get latest uri: '+ sys.exc_info()[0])
        return latestUri, response

    def getLatest(self):
        """
        Does a GET on the 'latest' uri from a GET on a channel, returning the data at the location.
        Returns data, GET response.
        """
        cnMeta, getRes = self.get({})

        if (None == cnMeta):
            return None, getRes

        data = response = None
        try:
            response = requests.get(cnMeta.latest)
            if (fs.isHTTPRedirect(response.status_code)):
                data = response.text
        except:
            print('Error getting data from /latest: '+ sys.exc_info()[0])
        return data, response

    def patch(self, callParams, headers={'content-type': 'application/json'}):
        """
        Updates a channel.
        Returns instance of ChannelMetadata, response.
        """
        self.headers = headers
        self.response = fs.FsResource.patch(callParams, self.headers)
        cnMeta = None

        if (fs.isHTTPSuccess(self.response.status_code)):
            cnMeta = ChannelMetadata(self.response.json())
        else:
            print('Did not get success response from PATCH: ', self.response.status_code)
        return cnMeta, self.response

    # NOT IMPLEMENTED METHODS: VVVVV
    def head(self, *args):
        raise NotImplementedError

    def delete(self, *args):
        raise NotImplementedError

    def put(self, *args):
        raise NotImplementedError

class Item(fs.FsResource):
    """
    To be created as the result of a POST to a Channel.
    Inherits GET, HEAD
    """
    def __init__(self, uri, debug=False):
        parsed = urlparse(uri)
        fs.FsResource.__init__(self, domain=parsed.netloc, debug=debug, uri=uri)

    # NOT IMPLEMENTED METHODS: VVVVV

    def delete(self, *args):
        raise NotImplementedError

    def patch(self, *args):
        raise NotImplementedError

    def post(self, *args):
        raise NotImplementedError

    def put(self, *args):
        raise NotImplementedError


def testMe():
    print 'Testing DH_helpers module'

    print('Instantiating DataHub')
    dh = DataHub(debug=True)

    print('Creating new channel...')
    cnName = txu.text_generator(min=10, includeSpace=False)
    cn, createRes = dh.post({'name': cnName})

    print('Creation result code: ', createRes.status_code)
    if (not fs.isHTTPSuccess(createRes.status_code)):
        print(createRes.text)
        return
    else:
        print('...created channel.')

    print('Getting channels')
    channels, getRes = dh.get({})

    print('List of channels: ')
    for chan in channels:
        print chan

    print('Fetching channel...')
    cnMeta, fetchRes = cn.get({})
    if (not fs.isHTTPSuccess(fetchRes.status_code)):
        print(fetchRes.text)
        return
    else:
        print('...fetched channel.')

    print('Inserting data...')
    myItem, postRes = cn.post('this is my data')
    if (not fs.isHTTPSuccess(postRes.status_code)):
        print(postRes.text)
        return
    else:
        print('...inserted data to ', myItem.uri)

    print('Fetching data...')
    getRes = myItem.get({})
    if (not fs.isHTTPSuccess(getRes.status_code)):
        print(getRes.text)
        return
    else:
        print('...fetched data.')
        print('Data: ', getRes.text)




if '__main__' == __name__:
    testMe()

