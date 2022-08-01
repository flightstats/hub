let request = require('request');

/*
  usage:
    node delete_objects.js [hubURL] [namePrefix] [objectType]

  examples:
    node delete_objects.js hub.iad.dev.flightstats.io TeSt_0_ webhook
    node delete_objects.js hub.pdx.dev.flightstats.io foo_ channel
 */

let hubURL = process.argv[2] || 'hub.dls.dev.flightstats.io';
let namePrefix = process.argv[3] || 'test_automatedjs_0_';
let objectType = process.argv[4] || 'channel';
let objectTypePlural = `${objectType}s`;

async function main() {
  console.log(`deleting ${objectTypePlural} that start with '${namePrefix}' at ${hubURL}`);
  let response = await httpGet(`http://${hubURL}/${objectType}`);
  if (response.statusCode !== 200) {
    console.log(`unable to get a list of ${objectTypePlural}`);
    return;
  }
  let objects = response.body._links[objectTypePlural]
    .filter(object => object.name.startsWith(namePrefix))
    .map(object => object.name);
  console.log(`${objectTypePlural} found:`, objects.length);

  for (let i = 0; i < objects.length; i++) {
    await httpDelete(`http://${hubURL}/internal/${objectType}/${objects[i]}`);
  }
}

main();


// ----------------------------------------


async function httpGet(url) {
  return httpRequest('GET', url, {'Content-Type': 'application/json'});
}

async function httpDelete(url) {
  return httpRequest('DELETE', url);
}

async function httpRequest(method, url, headers) {
  let options = {
    method: method,
    url: url,
    headers: headers || {},
    json: true,
    followRedirect: false
  };

  return new Promise((resolve, reject) => {
    console.log(method, '>', options.url, options.headers);

    request(options, (error, response) => {
      if (error) {
        reject(error);
      } else {
        console.log(method, '<', options.url, response.statusCode);
        resolve(response);
      }
    });
  });
}