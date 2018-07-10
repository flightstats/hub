// we have several places we are pulling the same exact keys out of response objects
// adding a couple reusable helpers here
const { fromObjectPath, getProp } = require('./functional');

module.exports.getStatusCode = getProp('statusCode'); // ~ function(obj) => obj.statusCode || null;
module.exports.getUris = fromObjectPath(['_links', 'uris']); // ~ function(obj) => obj._links.uris || null;
module.exports.getResponseBody = getProp('body'); // ~ function(obj) => obj.body || null;
