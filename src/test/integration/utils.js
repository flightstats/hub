require('./integration_config.js');
var frisby = require('frisby');
var http = require('http');
var fs = require('fs');

function ensureTestChannelCreated(channelName){
	console.info('Ensuring that test channel has been created...');
	frisby.create('Ensuring that the test channel exists.')
		.post(channelUrl, null, { body: JSON.stringify({ "name": channelName})})
		.addHeader("Content-Type", "application/json")
		.toss();
}

function download(url, completionHandler){
	http.get(url, function(res){
		var imagedata = '';
		res.setEncoding('binary');

		res.on('data', function(chunk){
			imagedata += chunk
		});

		res.on('end', function(){ completionHandler(imagedata); });
	});
}

exports.ensureTestChannelCreated = ensureTestChannelCreated;
exports.download = download;
