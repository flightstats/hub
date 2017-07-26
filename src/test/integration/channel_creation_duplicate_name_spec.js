require('./integration_config.js');

var channelName = utils.randomChannelName();
var channelResource = channelUrl + '/' + channelName;

describe(__filename, function () {

	it('verifies the channel doesn\'t exist yet', function (done) {
		utils.httpGet(channelResource)
			.then(function (response) {
				expect(response.statusCode).toEqual(404);
			})
			.catch(function (error) {
				expect(error).toBeNull();
			})
			.fin(function () {
				done();
			});
	});
	
	it('creates the channel', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': channelName};
		
		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(response.statusCode).toEqual(201);
			})
			.catch(function (error) {
				expect(error).toBeNull();
			})
			.fin(function () {
				done();
			});
	});
	
	it('verifies creating a channel with an existing name returns an error', function (done) {
		var url = channelUrl;
		var headers = {'Content-Type': 'application/json'};
		var body = {'name': channelName};

		utils.httpPost(url, headers, body)
			.then(function (response) {
				expect(response.statusCode).toEqual(409);
			})
			.catch(function (error) {
				expect(error).toBeNull();
			})
			.fin(function () {
				done();
			});
	});

});
