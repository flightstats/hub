const aws = require('aws-sdk');
const s3 = new aws.S3();

const bucket = null;

// format: {channel}/{year}/{month}/{day}/{hour}/{minute}
const batchPath = null;

main();

async function main() {
    if (!bucket) throw Error('no bucket specified');
    if (!batchPath) throw Error('no batch path specified');

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
