const aws = require('aws-sdk');
const s3 = new aws.S3();

const bucket = 'hub-v2-prod-east1.flightstats.com';
const batchPath = 'positionsFlightradar24/2018/10/16/15/52';

main();

async function main() {
    let channelName = batchPath.slice(0, batchPath.indexOf('/'));
    let metadataPath = batchPath.replace(channelName, channelName + 'Batch/index');
    let dataPath = batchPath.replace(channelName, channelName + 'Batch/items');

    await remove(bucket, metadataPath);
    await remove(bucket, dataPath);
}

async function remove(bucket, key) {
    console.log('DELETE >', bucket, key);
    let response = await s3Delete({Bucket: bucket, Key: key});
    console.log('DELETE <', bucket, key);
    return response;
}


function s3Delete(params) {
    return new Promise((resolve, reject) => {
        s3.deleteObject(params, (error, data) => {
            if (error) reject(error);
            resolve(data);
        });
    });
}
