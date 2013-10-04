__author__ = 'gnewcomb'


import sys
import requests
import json
import datetime as dt

def isHTTPSuccess(code):
    return ('2' == str(code)[0])

def isHTTPRedirect(code):
    return ('3' == str(code)[0])

def isHTTPServerError(code):
    return ('5' == str(code)[0])

def parseFSDateTime(x):
    """
    Parse the datetime returned in various DH calls, e.g., "2013-05-19T02:58:07.456Z"
    """
    myDt = dt.datetime(int(x[0:4]), int(x[5:7]), int(x[8:10]), int(x[11:13]), int(x[14:16]), int(x[17:19]), int(x[20:23]))
    return myDt

def areDatesClose(pA, pB, tolerance_minutes=5):
    """
    Given two datetimes (datetime.datetime), check that they are within 5 minutes of each other
    """
#    pA = parseFSDateTime(a)
#    pB = parseFSDateTime(b)
    if  (pA.year == pB.year and pA.month == pB.month and pA.day == pB.day):
        aMin = (pA.hour * 60) + pA.minute
        bMin = (pB.hour * 60) + pB.minute
        if ((aMin + (tolerance_minutes + 1)) > bMin) and ((aMin - (tolerance_minutes + 1) < bMin)):
            return True
        else:
            return False
    else:
        return False

# Abstract class for the specific calls
class FsResource:
    def __init__(self, domain=None, debug=False, uri=None):
        self.domain = domain
        self.debug = debug
        self.uri = uri

    def report(self):
        print 'uri: ', self.uri
        print 'payload: ', self.payload
        print 'headers: ', self.headers

    def __initCall(self, method, headers, payload, debug=False):
        self.headers = headers
        self.payload = payload
        if (debug) or (self.debug):
            self.report()
        print 'Calling {} at {}'.format(method, self.uri)

    # Common code for basic calls. To be overridden when anything special is needed.
    def __execCall(self, method):
        method = method.upper()

        try:
            if ('GET' == method):
                r = requests.get(self.uri, data=self.payload, headers=self.headers)
            elif ('PATCH' == method):
                r = requests.patch(self.uri, data=self.payload, headers=self.headers)
            elif ('PUT' == method):
                r = requests.put(self.uri, data=self.payload, headers=self.headers)
            elif ('POST' == method):
                r = requests.post(self.uri, data=self.payload, headers=self.headers)
            elif ('DELETE' == method):
                r = requests.delete(self.uri, data = self.payload, headers=self.headers)
            elif ('HEAD' == method):
                r = requests.head(self.uri, data=self.payload, headers = self.headers)
            else:
                raise ValueError
        except:
            print 'Error in {} call at {}: {}'.format(method, self.uri, sys.exc_info()[0])
        else:
            return r

    def get(self, callParams={}, headers={}, debug=False):
#        if (None == callParams):
#            callParams = {}
        method = 'GET'
        self.__initCall(method, headers, callParams, debug)
        return self.__execCall(method)

    def patch(self, callParams, headers={'content-type': 'application/json'}, debug=False):
        method = 'PATCH'
        self.__initCall(method, headers, json.dumps(callParams), debug)
        return self.__execCall(method)

    def post(self, callParams, headers={'content-type': 'application/json'}, debug=False):
        method = 'POST'
        self.__initCall(method, headers, json.dumps(callParams), debug)
        return self.__execCall(method)

    def put(self, callParams, headers={'content-type': 'application/json'}, debug=False):
        method = 'PUT'
        self.__initCall(method, headers, json.dumps(callParams), debug)
        return self.__execCall(method)

    def delete(self, callParams, headers={}, debug=False):
        method = 'DELETE'
        self.__initCall(method, headers, callParams, debug)
        return self.__execCall(method)

    def head(self, callParams, headers={}, debug=False):
        method = 'HEAD'
        self.__initCall(method, headers, callParams, debug)
        return self.__execCall(method)