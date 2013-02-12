
process.argv.forEach(function (val, index, array) {
  console.log(index + ': ' + val);
});


require('./integration_config.js');
var frisby = require('frisby');

var jsonBody = JSON.stringify({ "name": "testcase"});
var channelResource = channelUrl + "/testcase";

console.info("Making sure channel resource does not yet exist.");
frisby.create('Making sure channel resource does not yet exist.')
	.get(channelResource)
	.expectStatus(200)
.toss();

console.info("Testing basic channel creation response.")

frisby.create('Test basic channel creation')
	.post(channelUrl, null, { body: jsonBody})
	.addHeader("Content-Type", "application/json")
	.expectStatus(200)
	.expectHeader('content-type', 'application/json')
	.expectJSON({
		_links: {
			self: {
				href: channelResource
			}
		},
		name: "testcase"
		//TODO: Date validation
	})
	.afterJSON(function(result){
		console.info("Fetching channel resource to ensure that it exists...");
		frisby.create('Making sure channel resource exists.')
			.get(result['_links']['self']['href'])
			.expectStatus(200)
		.toss();
	})
.inspectJSON()
.toss();

