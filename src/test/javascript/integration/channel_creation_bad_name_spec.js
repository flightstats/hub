require('../integration_config');

describe(__filename, function () {

	it('creates the channel without a name', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': ''};

		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(response.statusCode).toEqual(400);
			})
			.finally(done);
	});

});
