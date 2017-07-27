require('./integration_config.js');

describe(__filename, function () {

	it('creates a channel with no payload', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = '';

		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(response.statusCode).toEqual(400);
			})
			.catch(function (error) {
				expect(error).toBeNull();
			})
			.fin(function () {
				done();
			});
	});

});
