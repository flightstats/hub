// writes to console log if doDebug is true *or* if only one param (msg) is provided
var debugLog = function(msg, doDebug) {
    if ((arguments.length < 2) || (true === doDebug))  {
        console.log(msg);
    }
};
exports.debugLog = debugLog;

// Includes only uninherited properties. Returns array of keys.
function getDictKeys(d) {
    var keys = [];

    for (var x in d) {
        if (d.hasOwnProperty(x)) {
            keys.push(x);
        }
    }
    return keys;
}
exports.getDictKeys = getDictKeys;

// Very lightweight (read: weak) comparison that assumes dictionary of k:v structure,
//   where v = non-complex value; no nesting allowed.
function dictCompare(d1, d2) {
    var d1keys, d2keys;

    d1keys = getDictKeys(d1);
    d2keys = getDictKeys(d2);

    if (d1keys.length != d2keys.length) return false;

    for (var i = 0; i < d1keys.length;i += 1) {
        var k = d1keys[i];

        if (!d2.hasOwnProperty(k)) {
            return false;
        }

        if (d1[k] !== d2[k]) {
            return false;
        }
    }

    return true;
}
exports.dictCompare = dictCompare;

// Some HTTP response helpers
var HTTPresponses = {
    "Continue":100
    ,"Switching_Protocols":101
    ,"Processing":102
    ,"OK":200
    ,"Created":201
    ,"Accepted":202
    ,"Non-Authoritative_Information":203
    ,"No_Content":204
    ,"Reset_Content":205
    ,"Partial_Content":206
    ,"Multi-Status":207
    ,"Already_Reported":208
    ,"Low_on_Storage_Space":250
    ,"IM_Used":226
    ,"Multiple_Choices":300
    ,"Moved_Permanently":301
    ,"Found":302
    ,"See_Other":303
    ,"Not_Modified":304
    ,"Use_Proxy":305
    ,"Switch-Proxy":306
    ,"Temporary_Redirect":307
    ,"Permanent_Redirect":308
    ,"Bad_Request":400
    ,"Unauthorized":401
    ,"Payment_Required":402
    ,"Forbidden":403
    ,"Not_Found":404
    ,"Method_Not_Allowed":405
    ,"Not_Acceptable":406
    ,"Proxy_Authentication_Required":407
    ,"Request_Timeout":408
    ,"Conflict":409
    ,"Gone":410
    ,"Length_Required":411
    ,"Precondition_Failed":412
    ,"Request_Entity_Too_Large":413
    ,"Request-URI_Too_Long":414
    ,"Unsupported_Media_Type":415
    ,"Requested_Range_Not_Satisfiable":416
    ,"Expectation_Failed":417
    ,"I'm_a_teapot":418
    ,"Enhance_Your_Calm":420
    ,"Upgrade_Required":426
    ,"Precondition_Required":428
    ,"Too_Many_Requests":429
    ,"Request_Header_Fields_Too_Large":431
    ,"Internal_Server_Error":500
};
exports.HTTPresponses = HTTPresponses;

// captures any type of error
var isHTTPError = function(code) {
    return ((code >= 400) && (code < 600));
};
exports.isHTTPError = isHTTPError;

var isHTTPClientError = function(code) {
    return ((code >= 400) && (code < 500));
};
exports.isHTTPClientError = isHTTPClientError;

var isHTTPServerError = function(code) {
    return ((code >= 500) && (code < 600));
};
exports.isHTTPServerError = isHTTPServerError;

var isHTTPSuccess = function(code) {
    return ((code >= 200) && (code < 300));
};
exports.isHTTPSuccess = isHTTPSuccess;

var isHTTPRedirect = function(code) {
    return ((code >= 300) && (code < 400));
};
exports.isHTTPRedirect = isHTTPRedirect;


// Read all of an object's properties and return them as an array -- to be used for confirming object structure
function getAllProperties(myObject) {
    var allProps = [];

    var recursiveIteration = function(object, parentName) {
        for (var property in object) {
            if (object.hasOwnProperty(property)) {
                var name = ('' == parentName) ? property : parentName +'.'+ property;

                if (typeof object[property] == "object") {
                    allProps.push(name);
                    recursiveIteration(object[property], name);
                }
                else {
                    allProps.push(name);
                }
            }
        }
    }

    recursiveIteration(myObject,'');

    return allProps;
}
exports.getAllProperties = getAllProperties;
