/*
 This allows us to decouple endpoints and configuration from tests.  Barely.
 For now it still requires the server to be running on localhost.  We will
 make this better over time.
 */
//dataHubUrlBase = 'http://datahub-01.cloud-east.dev:8080';
dataHubUrlBase = 'http://localhost:8080';
channelUrl = dataHubUrlBase + '/channel';
