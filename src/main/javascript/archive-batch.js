const aws = require('aws-sdk');
const s3 = new aws.S3();

const originBucket = 'hub-v2-prod-east1.flightstats.com';
const destinationBucket = 'archive-hub-v2-prod-east1.flightstats.com';
const batchPath = 'positionsFlightradar24/2018/10/16/15/52';

main();

async function main() {
    let channelName = batchPath.slice(0, batchPath.indexOf('/'));
    let metadataPath = batchPath.replace(channelName, channelName + 'Batch/index');
    let dataPath = batchPath.replace(channelName, channelName + 'Batch/items');

    // get metadata & zip
    let metadata = await get(originBucket, metadataPath);
    let data = await get(originBucket, dataPath);

    // archive metadata & zip
    await post(destinationBucket, metadataPath, metadata.Body);
    await post(destinationBucket, dataPath, data.Body);
}

async function get(bucket, key) {
    console.log('GET >', bucket, key);
    let response = await s3Get({Bucket: bucket, Key: key});
    console.log('GET <', bucket, key, response.ContentType, response.ContentLength);
    return response;
}

async function post(bucket, key, data) {
    console.log('POST >', bucket, key);
    let response = await s3Post({Bucket: bucket, Key: key, Body: data});
    console.log('POST <', bucket, key);
    return response;
}

function s3Get(params) {
    return new Promise((resolve, reject) => {
        s3.getObject(params, (error, data) => {
            if (error) reject(error);
            resolve(data);
        });
    });
}

function s3Post(params) {
    return new Promise((resolve, reject) => {
        s3.putObject(params, (error, data) => {
            if (error) reject(error);
            resolve(data);
        });
    });
}
