
var applicationTypes = [
    'application/atom+xml'
    ,'application/ecmascript'
    ,'application/json'
    ,'application/javascript'
    ,'application/octet-stream'
    ,'application/ogg'
    ,'application/pdf'
    ,'application/postscript'
    ,'application/rdf+xml'
    ,'application/rss+xml'
    ,'application/soap+xml'
    ,'application/xhtml+xml'
    ,'application/xml'
    ,'application/xml-dtd'
    ,'application/xop+xml'
    ,'application/zip'
    ,'application/gzip'
    ,'application/vnd.oasis.opendocument.text'
    ,'application/vnd.oasis.opendocument.graphics'
];

var imageTypes = [
    'image/gif'
    ,'image/jpeg'
    ,'image/pjpeg'
    ,'image/png'
    ,'image/svg+xml'
    ,'image/tiff'
    ,'image/vnd.microsoft.icon'
];

var messageTypes = [
    'message/http'
    ,'message/imdn+xml'
    ,'message/partial'
    ,'message/rfc822'
];

var textTypes = [
    'text/cmd'
    ,'text/css'
    ,'text/csv'
    ,'text/html'
    ,'text/javascript'
    ,'text/plain'
    ,'text/vcard'
    ,'text/xml'
];



//// LESS COMMON or tricky

// [edit]Type multipart
// For archives and other objects made of more than one part.

var otherContentTypes = [
    'application/EDI-X12'
    ,'application/EDIFACT'
    ,'application/font-woff'
    ,'audio/basic'
    ,'audio/L24'
    ,'audio/mp4'
    ,'audio/mpeg'
    ,'audio/ogg'
    ,'audio/vorbis'
    ,'audio/vnd.rn-realaudio'
    ,'audio/vnd.wave'
    ,'audio/webm'
    ,'model/example'
    ,'model/iges'
    ,'model/mesh'
    ,'model/vrml'
    ,'model/x3d+binary'
    ,'model/x3d+vrml'
    ,'model/x3d+xml'
    ,'video/mpeg'
    ,'video/mp4'
    ,'video/ogg'
    ,'video/quicktime'
    ,'video/webm'
    ,'video/x-matroska'
    ,'video/x-ms-wmv'
    ,'video/x-flv'
    ,'application/vnd.oasis.opendocument.spreadsheet'
    ,'application/vnd.oasis.opendocument.presentation'
    ,'application/vnd.ms-excel'
    ,'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ,'application/vnd.ms-powerpoint'
    ,'application/vnd.openxmlformats-officedocument.presentationml.presentation'
    ,'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ,'application/vnd.mozilla.xul+xml'
    ,'application/vnd.google-earth.kml+xml'
    ,'multipart/mixed'
    ,'multipart/alternative'
    ,'multipart/related'
    ,'multipart/form-data'
    ,'multipart/signed'
    ,'multipart/encrypted'
    ,'application/x-deb'
    ,'application/x-dvi'
    ,'application/x-font-ttf'
    ,'application/x-javascript'
    ,'application/x-latex'
    ,'application/x-mpegURL'
    ,'application/x-rar-compressed'
    ,'application/x-shockwave-flash'
    ,'application/x-stuffit'
    ,'application/x-tar'
    ,'application/x-www-form-urlencoded'
    ,'application/x-xpinstall'
    ,'audio/x-aac'
    ,'audio/x-caf'
    ,'image/x-xcf'
    ,'text/x-gwt-rpc'
    ,'text/x-jquery-tmpl'
    ,'application/x-pkcs12'
    ,'application/x-pkcs12'
    ,'application/x-pkcs7-certificates'
    ,'application/x-pkcs7-certificates'
    ,'application/x-pkcs7-certreqresp'
    ,'application/x-pkcs7-mime'
    ,'application/x-pkcs7-mime'
    ,'application/x-pkcs7-signature'
];


module.exports.applicationTypes = applicationTypes;
module.exports.imageTypes = imageTypes;
module.exports.messageTypes = messageTypes;
module.exports.textTypes = textTypes;

module.exports.nonDHContentTypes = otherContentTypes;