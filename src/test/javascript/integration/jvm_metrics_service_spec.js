const bodyParser = require('body-parser');
const express = require('express');
const http = require('http');
const {
    closeServer,
    fromObjectPath,
    getProp,
    hubClientGet,
    itSleeps,
} = require('../lib/helpers');
const { getHubUrlBase, getIp } = require('../lib/config');

const headers = {
    'Content-Type': 'application/json',
    Accept: "application/json",
};
const sharedContext = {
    queryResults: [],
    payloadResults: [],
    server: null,
};

const startMockInfluxDb = async () => {
    const app = express();

    app.use(bodyParser.raw({type: 'application/x-www-form-urlencoded', limit: '2mb' }));

    app.post('/write', (request, response) => {
        const body = getProp('body',request);
        const metrics = Buffer.from(body).toString();
        const query = fromObjectPath(['query', 'db'], request);
        const metricsArray = metrics && metrics.split('\n');
        sharedContext.queryResults.push(query);
        sharedContext.payloadResults.push(...metricsArray);
        response.sendStatus(200);
    });

    const server =  new http.Server(app);

    server.on('connection', (socket) => {
        socket.setTimeout(1000);
    });

    server.on('request', function (request, response) {
       request.on('end', function () {
            response.end();
        });
    });

    const callbackDomain = getIp();

    server.listen(8086, callbackDomain);
    sharedContext.server = await server.on('listening', () => {
        console.log(`server listening at http://${getIp()}:8086/write`);
        return server;
    });
};

const EXPECTED_METRIC_TAGS = [
    'jvm_gc.G1-Old-Generation',
    'jvm_gc.G1-Young-Generation',
    'jvm_memory.heap',
    'jvm_memory.non-heap',
    'jvm_memory.pools',
    'jvm_memory.total',
    'jvm_thread',
];

const EXPECTED_HOST_TAGS = [
    'cluster=local-single',
    'env=single',
    'role=hub',
    'team=ddt',
    'version=local',
];

describe(__filename, function () {
    beforeAll(async () => {
        const request = await hubClientGet(`${getHubUrlBase()}/internal/properties`, headers);
        const properties = fromObjectPath(['body', 'properties'], request);
        let intervalInSeconds = properties && properties['metrics.seconds'];
        intervalInSeconds = parseInt(intervalInSeconds, 10);
        // it starts a mock influxdb endpoint
        await startMockInfluxDb();
        // it waits the reporting interval for a metric dump
        const waitTimeMillis = (intervalInSeconds + 1) * 1000 * 2;
        await itSleeps(waitTimeMillis);
    });

    it('has the correct database name in the query',() => {
        const { queryResults } = sharedContext;
        expect(queryResults.length).toBeGreaterThan(0);
        expect(queryResults.every(databaseName => databaseName === 'hubmain'));
    });

    it('has reported the metrics', () => {
        const { payloadResults } = sharedContext;
        expect(payloadResults.length).toBeGreaterThan(0);
    });

    it('has the expected host tags reported in each metric',() => {
        const { payloadResults } = sharedContext;
        expect(payloadResults.every(metricString => metricString.includes('host=')));
        expect(payloadResults.every(metricString => EXPECTED_HOST_TAGS.every(tag => metricString.includes(tag)))).toBeTruthy();
    });

    it('has the expected jvm tags reported in individual metrics', () => {
        const { payloadResults } = sharedContext;
        expect(EXPECTED_METRIC_TAGS.every(metricPartial => payloadResults.some(metric => metric.includes(metricPartial)))).toBeTruthy();
    });

    afterAll(async () => {
        const { server } = sharedContext;
        if (server) {
            closeServer(server);
        }
    });
});
