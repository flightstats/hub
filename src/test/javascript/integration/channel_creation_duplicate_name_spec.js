require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

	it('verifies the channel doesn\'t exist yet', function (done) {
		utils.httpGet(channelResource)
			.then(function (response) {
				expect(response.statusCode).toEqual(404);
			})
			.finally(done);
	});
	
	it('creates the channel', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': channelName};
		
		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(response.statusCode).toEqual(201);
			})
			.finally(done);
	});
	
	it('verifies creating a channel with an existing name returns an error', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': channelName};

		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(response.statusCode).toEqual(409);
			})
			.finally(done);
	});

});
