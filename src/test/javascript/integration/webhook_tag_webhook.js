require('../integration_config');

let tag = utils.randomTag();
let tagURL = `${hubUrlBase}/tag/${tag}`;
let tagWebhookPrototypeURL = `${utils.getWebhookUrl()}/TAGWHPROTO_${tag}`;

let channelOneName = utils.randomChannelName();
let channelOneURL = `${channelUrl}/${channelOneName}`;
let channelOneWebhookURL = `${utils.getWebhookUrl()}/TAGWH_${tag}_${channelOneName}`;

let channelTwoName = utils.randomChannelName();
let channelTwoURL = `${channelUrl}/${channelTwoName}`;
let channelTwoWebhookURL = `${utils.getWebhookUrl()}/TAGWH_${tag}_${channelTwoName}`;

let acceptJSON = {'Content-Type': 'application/json'};

describe(__filename, function () {

  it('creates a tag webhook prototype', (done) => {
    let config = {
      'callbackUrl': 'http://nothing/callback',
      'tagUrl': tagURL
    };
    utils.httpPut(tagWebhookPrototypeURL, acceptJSON, config)
      .then(response => expect(response.statusCode).toEqual(201))
      .finally(done);
  });

  it('verifies the tag webhook prototype exists', (done) => {
    utils.httpGet(tagWebhookPrototypeURL)
      .then(response => expect(response.statusCode).toEqual(200))
      .finally(done);
  });

  it(`creates channel one with tag ${tag}`, (done) => {
    let config = {'tags': [tag]};
    utils.httpPut(channelOneURL, acceptJSON, config)
      .then(response => expect(response.statusCode).toEqual(201))
      .finally(done);
  });

  it(`creates channel two with tag ${tag}`, (done) => {
    let config = {'tags': [tag]};
    utils.httpPut(channelTwoURL, acceptJSON, config)
      .then(response => expect(response.statusCode).toEqual(201))
      .finally(done);
  });

  utils.itSleeps(1000);

  it('verifies a webhook for channel one exists', (done) => {
    utils.httpGet(channelOneWebhookURL)
      .then(response => expect(response.statusCode).toEqual(200))
      .finally(done);
  });

  it('verifies a webhook for channel two exists', (done) => {
    utils.httpGet(channelTwoWebhookURL)
      .then(response => expect(response.statusCode).toEqual(200))
      .finally(done);
  });

  it('removes the tag from channel one', (done) => {
    let config = {'tags': []};
    utils.httpPut(channelOneURL, acceptJSON, config)
      .then(response => expect(response.statusCode).toEqual(201))
      .finally(done);
  });

  utils.itSleeps(1000);

  it('verifies the webhook created for channel one is removed', (done) => {
    utils.httpGet(channelOneWebhookURL)
      .then(response => expect(response.statusCode).toEqual(404))
      .finally(done);
  });

  it('removes the tag webhook prototype', (done) => {
    utils.httpDelete(tagWebhookPrototypeURL)
      .then(response => expect(response.statusCode).toEqual(202))
      .finally(done);
  });

  utils.itSleeps(1000);

  it('verifies the webhook created for channel two is removed', (done) => {
    utils.httpGet(channelTwoWebhookURL)
      .then(response => expect(response.statusCode).toEqual(404))
      .finally(done);
  });

});

