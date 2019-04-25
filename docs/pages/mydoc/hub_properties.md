---
title: HUB properties list
keywords: 
last_updated: April 26, 2019
tags: []
summary: 
sidebar: mydoc_sidebar
permalink: hub_properties.html
folder: hub
---

# Here is the complete list of hub properties that is configurable.

#App Properties

app.environment=dev
app.name=hub
app.url=
app.remoteTimeFile=/home/hub/remoteTime
app.birthDay=2015/01/01
app.encrypted=false
hub.protect.channels=true
hub.read.only=
app.stable_seconds=5
query.merge.max.wait.minutes=2
app.maxPayloadSizeMB=40
app.large.payload.MB=40
app.directionCountLimit=10000
hub.gcMinutes=60
hub.runGC=false
app.minPostTimeMillis=5
app.maxPostTimeMillis=1000
query.merge.max.wait.minutes=2
app.runNtpMonitor=true
app.shutdown_wait_seconds=180
app.shutdown_delay_seconds=60
cluster.location=local
hub.type=aws
http.connect.timeout.seconds=30
http.read.timeout.seconds=120
http.maxRetries=8
http.sleep=1000
http.bind_port=8080
logSlowTracesSeconds=10
traces.limit=50
LastContentPathTracing=channelToTrace

#AWS properties
aws.credentials=hub_test_credentials.properties
aws.protocol=HTTP
aws.retry.delay.millis=100
aws.retry.max.delay.millis=20000
aws.retry.unknown.host.delay.millis=5000
aws.signing_region=us-east-1

#Datadog properties
metrics.datadog.url=https://app.datadoghq.com/api/v1
metrics.data_dog.app_key=
metrics.data_dog.api_key=
metrics.dogstatsd.port=8125

#Dynamo properties
dynamo.endpoint=dynamodb.us-east-1.amazonaws.com
dynamo.table_creation_wait_minutes=10
dynamo.throughput.channel.read=100
dynamo.throughput.channel.write=10
dynamo.throughput.webhook.read=100
dynamo.throughput.webhook.write=10
dynamo.maxConnections=50
dynamo.connectionTimeout=10000
dynamo.socketTimeout=30000
dynamo.table_name.webhook_configs=hub-local-GroupConfig
dynamo.table_name.channel_configs=hub-local-channelMetaData

#Spoke properties
storage.path=/file
spoke.enforceTTL=true
spoke.write.factor=3
spoke.read.ttlMinutes=
spoke.read.path=
spoke.write.ttlMinutes=
spoke.write.path=

#Spoke fallback properties
spoke.ttlMinutes=
spoke.path=

#Webhook properties
webhook.callbackTimeoutSeconds.min=1
webhook.callbackTimeoutSeconds.max=1800
webhook.callbackTimeoutSeconds.default=120
webhook.connectTimeoutSeconds=60
webhook.readTimeoutSeconds=60
webhook.shutdown.threads=100

#Zookeeper properties
zookeeper.connection=localhost:2181
zookeeper.baseSleepTimeMs=10
zookeeper.maxSleepTimeMs=10000
zookeeper.maxRetries=20
zookeeper.maxRetries=20

#Tick Properties
metrics.influxdb.host=localhost
metrics.influxdb.port=8086
metrics.statsd.port=8124
metrics.influxdb.protocol=http
metrics.influxdb.database.name=hub_tick
metrics.influxdb.database.password=
metrics.influxdb.database.user=
metrics.enable=false
metrics.seconds=15
metrics.tags.role=hub
metrics.tags.team=development

#S3 Properties
s3.environment=local
s3.endpoint=s3-external-1.amazonaws.com
s3.bucket_name=hub-local
s3.maxRules=defaultValue
s3.pathStyleAccessEnable=false
s3.disableChunkedEncoding=false
s3.writeQueueSize=40000
s3.writeQueueThreads=20
s3.maxConnections=50
s3.connectionTimeout=10 * 1000
s3.socketTimeout=30 * 1000
s3Verifier.run=true
s3.maxQueryItems=1000
s3Verifier.baseTimeoutMinutes=2
s3Verifier.offsetMinutes=15
s3Verifier.channelThreads=3
channel.enforceTTL=false
s3.large.threads=3
s3.maxChunkMB=40
