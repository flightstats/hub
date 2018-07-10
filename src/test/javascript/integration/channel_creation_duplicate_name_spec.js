require('../integration_config');
const { getStatusCode } = require('../lib/helpers');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

	it('verifies the channel doesn\'t exist yet', function (done) {
		utils.httpGet(channelResource)
			.then(function (response) {
				expect(getStatusCode(response)).toEqual(404);
			})
			.finally(done);
	});

	it('creates the channel', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': channelName};

		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(getStatusCode(response)).toEqual(201);
			})
			.finally(done);
	});

	it('verifies creating a channel with an existing name returns an error', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': channelName};

		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(getStatusCode(response)).toEqual(409);
			})
			.finally(done);
	});

});
