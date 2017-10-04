require('../integration_config');

var channelName = utils.randomChannelName();
var channelResource = `${channelUrl}/${channelName}`;
var jsonContent = {"Content-Type": "application/json"};

describe(__filename, () => {

    it('creates a batch channel', (done) => {
        utils.httpPut(channelResource, jsonContent, {storage: 'BATCH'})
            .then(response => expect(response.statusCode).toEqual(201))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    let itemURL;
    let itemURLSansMS;

    it('inserts an item into the channel', (done) => {
        utils.httpPost(channelResource, {}, `${Date.now()}`)
            .then(response => {
                expect(response.statusCode).toEqual(201);
                itemURL = response.body._links.self.href;
                console.log('url:', itemURL);
                let itemURLSansHash = itemURL.slice(0, itemURL.lastIndexOf('/'));
                itemURLSansMS = itemURLSansHash.slice(0, itemURLSansHash.lastIndexOf('/'));
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('receives an empty list from the batch cache', (done) => {
        let url = `${itemURLSansMS}?location=CACHE_BATCH`;
        utils.httpGet(url)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                let uris = response.body._links.uris;
                console.log('uris:', uris);
                expect(uris.length).toEqual(0);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('gets the item from the default sources', (done) => {
        utils.httpGet(itemURLSansMS)
            .then(response => expect(response.statusCode).toEqual(200))
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

    it('receives a 200 trying to get the item from the batch cache', (done) => {
        let url = `${itemURLSansMS}?location=CACHE_BATCH`;
        utils.httpGet(url)
            .then(response => {
                expect(response.statusCode).toEqual(200);
                let uris = response.body._links.uris;
                console.log('uris:', uris);
                expect(uris.length).toEqual(1);
                expect(uris[0]).toEqual(itemURL);
            })
            .catch(error => expect(error).toBeNull())
            .finally(done);
    });

});
